## Setup

### Hardware and System Information

```console
$ uname -a
Linux dev-server 6.8.0-107-generic #107-Ubuntu SMP PREEMPT_DYNAMIC Fri Mar 13 19:51:50 UTC 2026 x86_64 x86_64 x86_64 GNU/Linux
```

```console
$ cat /proc/cpuinfo
processor       : 19
vendor_id       : GenuineIntel
cpu family      : 6
model           : 165
model name      : Intel(R) Core(TM) i9-10900 CPU @ 2.80GHz
stepping        : 5
microcode       : 0x100
cpu MHz         : 4700.042
cache size      : 20480 KB
physical id     : 0
siblings        : 20
core id         : 9
cpu cores       : 10
apicid          : 19
initial apicid  : 19
fpu             : yes
fpu_exception   : yes
cpuid level     : 22
wp              : yes
flags           : fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm pbe syscall nx pdpe1gb rdtscp lm constant_tsc art arch_perfmon pebs bts rep_good nopl xtopology nonstop_tsc cpuid aperfmperf pni pclmulqdq dtes64 monitor ds_cpl vmx smx est tm2 ssse3 sdbg fma cx16 xtpr pdcm pcid sse4_1 sse4_2 x2apic movbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand lahf_lm abm 3dnowprefetch cpuid_fault epb ssbd ibrs ibpb stibp ibrs_enhanced tpr_shadow flexpriority ept vpid ept_ad fsgsbase tsc_adjust bmi1 avx2 smep bmi2 erms invpcid mpx rdseed adx smap clflushopt intel_pt xsaveopt xsavec xgetbv1 xsaves dtherm ida arat pln pts hwp hwp_notify hwp_act_window hwp_epp vnmi pku ospke md_clear flush_l1d arch_capabilities ibpb_exit_to_user
vmx flags       : vnmi preemption_timer posted_intr invvpid ept_x_only ept_ad ept_1gb flexpriority apicv tsc_offset vtpr mtf vapic ept vpid unrestricted_guest vapic_reg vid ple shadow_vmcs pml ept_mode_based_exec
bugs            : spectre_v1 spectre_v2 spec_store_bypass swapgs itlb_multihit srbds mmio_stale_data retbleed eibrs_pbrsb gds bhi its vmscape
bogomips        : 5599.85
clflush size    : 64
cache_alignment : 64
address sizes   : 39 bits physical, 48 bits virtual
power management:
```

```console
$ fastfetch --logo none
lqhuang@dev-server
------------------
OS: Ubuntu 24.04.4 LTS (Noble Numbat) x86_64
Kernel: Linux 6.8.0-107-generic
Uptime: 13 days, 47 mins
Packages: 1015 (dpkg), 56 (nix-default)
Shell: zsh 5.9
Terminal: tmux 3.4
CPU: Intel(R) Core(TM) i9-10900 (20) @ 5.20 GHz
GPU 1: Intel UHD Graphics 630 @ 1.20 GHz [Integrated]
Memory: 35.37 GiB / 60.47 GiB (58%)
Swap: 355.75 MiB / 16.00 GiB (2%)
Disk (/): 354.54 GiB / 899.56 GiB (39%) - xfs
Locale: en_US.UTF-8
```

## Software Versions

```console
$ java -version
openjdk version "25.0.1" 2025-10-21 LTS
OpenJDK Runtime Environment Temurin-25.0.1+8 (build 25.0.1+8-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.1+8 (build 25.0.1+8-LTS, mixed mode, sharing)
```

```
reload; \
clean; \
set nativeConfig ~=  { _.withMode(scala.scalanative.build.Mode.releaseFast).withOptimize(true) }; \
set ThisBuild/nativeConfig ~=  { _.withMode(scala.scalanative.build.Mode.releaseFast).withOptimize(true) }; \
show sandbox3/nativeConfig; \
sandbox3/run
```

```sh-session
$ clang++ --version
Ubuntu clang version 18.1.3 (1ubuntu1)
Target: x86_64-pc-linux-gnu
Thread model: posix
InstalledDir: /usr/bin
```
