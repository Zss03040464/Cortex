#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
set -euo pipefail

cortex_repo=$(cd "$(dirname "$0")/.." && pwd)
cortex_ndk=${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}
if [[ -z "$cortex_ndk" ]]; then
    printf '%s\n' '请设置 ANDROID_NDK_HOME，指向 Android NDK r29。' >&2
    exit 64
fi
python3 - "$cortex_ndk/source.properties" "$cortex_repo/gradle/libs.versions.toml" <<'PY'
import pathlib, re, sys
properties = pathlib.Path(sys.argv[1])
catalog = pathlib.Path(sys.argv[2]).read_text()
version = re.search(r'^ndk\s*=\s*"([^"]+)"\s*$', catalog, re.MULTILINE)
if version is None:
    raise SystemExit('版本目录缺少 ndk 条目')
expected = version.group(1)
if not properties.is_file():
    raise SystemExit('无法读取 NDK source.properties：' + str(properties))
values = dict(line.split('=', 1) for line in properties.read_text().splitlines() if '=' in line)
revision = next((value.strip() for key, value in values.items() if key.strip() == 'Pkg.Revision'), '')
if revision != expected:
    raise SystemExit('此构建固定使用 NDK ' + expected + '，实际版本：' + (revision or '未知'))
PY
case $(uname -s) in
    Darwin) cortex_host=darwin-x86_64 ;;
    Linux) cortex_host=linux-x86_64 ;;
    *) printf '%s\n' '仅支持 macOS 或 Linux 构建主机。' >&2; exit 64 ;;
esac
cortex_toolchain="$cortex_ndk/toolchains/llvm/prebuilt/$cortex_host/bin"
cortex_sources=${CORTEX_NATIVE_SOURCES:-$cortex_repo/.analysis/cortex-native-sources}
cortex_build=${CORTEX_NATIVE_BUILD:-$cortex_repo/.analysis/cortex-native-build}
cortex_output="$cortex_repo/app/src/main/jniLibs"
cortex_api=34
trap 'printf "本地构建失败，请检查 %s 下的 configure.log / build.log。\n" "$cortex_build" >&2' ERR
mkdir -p "$cortex_sources" "$cortex_build/tools" "$cortex_output"
ln -sf "$cortex_toolchain/llvm-readelf" "$cortex_build/tools/readelf"
export PATH="$cortex_build/tools:$PATH"
export PYTHONHASHSEED=1
export PYTHON=python3
export LC_ALL=C

fetch_source() {
    local name=$1 url=$2 digest=$3 directory=$4
    local archive="$cortex_sources/$name.tar.gz"
    if [[ -f "$cortex_sources/$name.tgz" ]]; then
        archive="$cortex_sources/$name.tgz"
    fi
    if [[ ! -f "$archive" ]]; then
        if [[ -f "$cortex_repo/app/src/main/assets/native-sources/$name.tgz" ]]; then
            cp "$cortex_repo/app/src/main/assets/native-sources/$name.tgz" "$archive"
        else
            curl --fail --location --retry 3 "$url" -o "$archive.partial"
            mv "$archive.partial" "$archive"
        fi
    fi
    python3 - "$archive" "$digest" <<'PY'
import hashlib, pathlib, sys
actual = hashlib.sha256(pathlib.Path(sys.argv[1]).read_bytes()).hexdigest()
if actual != sys.argv[2]:
    raise SystemExit('源码 SHA-256 不匹配：' + sys.argv[1])
PY
    if [[ ! -d "$cortex_sources/$directory" ]]; then
        tar -xzf "$archive" -C "$cortex_sources"
    fi
}

fetch_source proot https://github.com/termux/proot/archive/refs/tags/v5.1.107.92.tar.gz \
    a1b070f55ec32b78e5033621476533d7230eb110275fe0cc3ee79c4fb334cfaa proot-5.1.107.92
fetch_source talloc https://www.samba.org/ftp/talloc/talloc-2.4.3.tar.gz \
    dc46c40b9f46bb34dd97fe41f548b0e8b247b77a918576733c528e83abd854dd talloc-2.4.3
fetch_source shmem https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz \
    1e5ff8459bc0a8c229dd8a94b27d119987e09ef3414331c2b5ebfff20b98e867 libandroid-shmem-0.7

for cortex_abi in arm64-v8a x86_64 armeabi-v7a x86; do
    case "$cortex_abi" in
        arm64-v8a) cortex_target=aarch64-linux-android ;;
        x86_64) cortex_target=x86_64-linux-android ;;
        armeabi-v7a) cortex_target=armv7a-linux-androideabi ;;
        x86) cortex_target=i686-linux-android ;;
    esac
    cortex_cc="$cortex_toolchain/$cortex_target$cortex_api-clang"
    cortex_abi_build="$cortex_build/$cortex_abi"
    mkdir -p "$cortex_abi_build" "$cortex_output/$cortex_abi"
    printf '编译 PTY：%s\n' "$cortex_abi"
    "$cortex_cc" -O2 -Wall -Wextra -Werror -fPIE -pie \
        -Wl,-z,max-page-size=16384 -Wl,-z,relro,-z,now \
        -ffile-prefix-map="$cortex_repo"=. \
        "$cortex_repo/app/src/main/cpp/cortex_pty.c" -o "$cortex_output/$cortex_abi/libcortex_pty.so"
    "$cortex_toolchain/llvm-strip" "$cortex_output/$cortex_abi/libcortex_pty.so"
    if [[ "$cortex_abi" != arm64-v8a && "$cortex_abi" != x86_64 ]]; then continue; fi

    cortex_prefix="$cortex_abi_build/prefix"
    mkdir -p "$cortex_prefix/include/sys" "$cortex_prefix/lib"
    # 在临时构建副本生成配置，固定源码缓存保持原样。
    if [[ ! -d "$cortex_abi_build/talloc" ]]; then
        cp -R "$cortex_sources/talloc-2.4.3" "$cortex_abi_build/talloc"
    fi
    cat > "$cortex_abi_build/talloc/cross-answers.txt" <<'ANSWERS'
Checking uname sysname type: "Linux"
Checking uname machine type: "dontcare"
Checking uname release type: "dontcare"
Checking uname version type: "dontcare"
Checking simple C program: OK
building library support: OK
Checking for large file support: OK
Checking for -D_FILE_OFFSET_BITS=64: OK
Checking for WORDS_BIGENDIAN: OK
Checking for C99 vsnprintf: OK
Checking for HAVE_SECURE_MKSTEMP: OK
rpath library support: OK
-Wl,--version-script support: FAIL
Checking correct behavior of strtoll: OK
Checking correct behavior of strptime: OK
Checking for HAVE_IFACE_GETIFADDRS: OK
Checking for HAVE_IFACE_IFCONF: OK
Checking for HAVE_IFACE_IFREQ: OK
Checking getconf LFS_CFLAGS: OK
Checking for large file support without additional flags: OK
Checking for working strptime: OK
Checking for HAVE_SHARED_MMAP: OK
Checking for HAVE_MREMAP: OK
Checking for HAVE_INCOHERENT_MMAP: OK
Checking getconf large file support flags work: OK
ANSWERS
    (
        cd "$cortex_abi_build/talloc"
        export CC="$cortex_cc" AR="$cortex_toolchain/llvm-ar"
        export CFLAGS="-O2 -fPIC -D__STDC_WANT_LIB_EXT1__=1 -ffile-prefix-map=$cortex_repo=."
        ./configure --prefix="$cortex_prefix" --disable-rpath --disable-python \
            --cross-compile --cross-answers=cross-answers.txt > configure.log 2>&1
        python3 buildtools/bin/waf build --targets=talloc > build.log 2>&1
        "$cortex_toolchain/llvm-ar" rcs "$cortex_prefix/lib/libtalloc.a" bin/default/talloc.c.*.o
        cp talloc.h "$cortex_prefix/include/talloc.h"
    )
    mkdir -p "$cortex_abi_build/shmem"
    cp "$cortex_sources/libandroid-shmem-0.7/shmem.c" "$cortex_sources/libandroid-shmem-0.7/shm.h" "$cortex_abi_build/shmem/"
    patch -s -p1 -d "$cortex_abi_build/shmem" < "$cortex_repo/scripts/native/shmem-app-temp.patch"
    "$cortex_cc" -O2 -fPIC -std=c11 -ffile-prefix-map="$cortex_repo"=. \
        -c "$cortex_abi_build/shmem/shmem.c" -o "$cortex_abi_build/shmem.o"
    "$cortex_toolchain/llvm-ar" rcs "$cortex_prefix/lib/libandroid-shmem.a" "$cortex_abi_build/shmem.o"
    cp "$cortex_sources/libandroid-shmem-0.7/shm.h" "$cortex_prefix/include/sys/shm.h"
    mkdir -p "$cortex_abi_build/proot"
    cp -R "$cortex_sources/proot-5.1.107.92/." "$cortex_abi_build/proot/"
    patch -s -p1 -d "$cortex_abi_build/proot" < "$cortex_repo/scripts/native/proot-bionic-headers.patch"
    (
        cd "$cortex_abi_build/proot/src"
        cortex_loader_address=$("$cortex_cc" -E -dM -DNO_LIBC_HEADER arch.h | awk '$2 == "LOADER_ADDRESS" {print $3}')
        make -j4 \
            CC="$cortex_cc" STRIP="$cortex_toolchain/llvm-strip" \
            OBJCOPY="$cortex_toolchain/llvm-objcopy" OBJDUMP="$cortex_toolchain/llvm-objdump" \
            GIT=false HAS_LOADER_32BIT= PROOT_UNBUNDLE_LOADER=/dev/null PROOT_WITH_LIBANDROID_SHMEM=1 \
            CPPFLAGS="-D_FILE_OFFSET_BITS=64 -D_GNU_SOURCE -I. -I$cortex_prefix/include" \
            CFLAGS="-O2 -Wall -Wextra -fPIE -fno-stack-protector -ffile-prefix-map=$cortex_repo=. -DWITH_LIBANDROID_SHMEM -DPROOT_UNBUNDLE_LOADER=\"\\\"/dev/null\\\"\" -DVERSION=\"\\\"5.1.107.92\\\"\"" \
            LDFLAGS="-pie -L$cortex_prefix/lib -ltalloc -landroid-shmem -landroid -llog -Wl,-z,noexecstack,-z,max-page-size=16384,-z,relro,-z,now" \
            LOADER_LDFLAGS="-static -nostdlib -Wl,--build-id=none,-Ttext=$cortex_loader_address,--rosegment,-z,noexecstack,-z,max-page-size=16384" \
            > build.log 2>&1
        cp proot "$cortex_output/$cortex_abi/libproot_exec.so"
        cp loader/loader "$cortex_output/$cortex_abi/libproot_loader.so"
        "$cortex_toolchain/llvm-strip" "$cortex_output/$cortex_abi/libproot_exec.so" "$cortex_output/$cortex_abi/libproot_loader.so"
    )
    printf 'PRoot 与配对 loader 已生成：%s\n' "$cortex_abi"
done

# 源码分发包从实际构建入口生成，避免 APK 内维护另一份脚本和补丁。
python3 - "$cortex_repo" <<'PY'
import gzip, io, pathlib, tarfile, sys
root = pathlib.Path(sys.argv[1])
paths = [root / 'scripts/build-terminal-native.sh', root / 'app/src/main/cpp/cortex_pty.c',
         root / 'gradle/libs.versions.toml']
paths += sorted((root / 'scripts/native').glob('*.patch'))
paths += [root / 'scripts/native/README.md']
destination = root / 'app/src/main/assets/native-sources/cortex-native-build.tgz'
with destination.open('wb') as raw, gzip.GzipFile(filename='', mode='wb', fileobj=raw, mtime=0) as compressed:
    with tarfile.open(fileobj=compressed, mode='w') as archive:
        for path in paths:
            data = path.read_bytes()
            info = tarfile.TarInfo(str(path.relative_to(root)))
            info.size = len(data)
            info.mode = 0o755 if path.suffix == '.sh' else 0o644
            archive.addfile(info, io.BytesIO(data))
PY

python3 - "$cortex_output" "$cortex_toolchain/llvm-readelf" <<'PY'
import hashlib, pathlib, re, subprocess, sys
root = pathlib.Path(sys.argv[1])
for path in sorted(root.glob('*/*.so')):
    headers = subprocess.check_output([sys.argv[2], '-lW', str(path)], text=True)
    alignments = [int(line.split()[-1], 16) for line in headers.splitlines() if line.lstrip().startswith('LOAD')]
    if not alignments or min(alignments) < 16384:
        raise SystemExit('ELF 未满足 16 KiB 页对齐：' + str(path))
    dynamic = subprocess.check_output([sys.argv[2], '-d', str(path)], text=True)
    libraries = re.findall(r'Shared library: \[(.+?)\]', dynamic)
    if set(libraries) - {'libc.so', 'libdl.so', 'libm.so', 'libandroid.so', 'liblog.so'}:
        raise SystemExit('ELF 存在未打包的动态依赖：' + str(path))
    print(hashlib.sha256(path.read_bytes()).hexdigest(), path.relative_to(root))
PY
