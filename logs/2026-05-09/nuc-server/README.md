# Setup

### Hardware and System Information

```console
$ uname -a
Linux nuc-server 6.12.57+deb13-amd64 #1 SMP PREEMPT_DYNAMIC Debian 6.12.57-1 (2025-11-05) x86_64 GNU/Linux
```

```console
$ /proc/cpuinfo
processor       : 11
vendor_id       : GenuineIntel
cpu family      : 6
model           : 166
model name      : Intel(R) Core(TM) i7-10710U CPU @ 1.10GHz
stepping        : 0
microcode       : 0x102
cpu MHz         : 2799.995
cache size      : 12288 KB
physical id     : 0
siblings        : 12
core id         : 5
cpu cores       : 6
apicid          : 11
initial apicid  : 11
fpu             : yes
fpu_exception   : yes
cpuid level     : 22
wp              : yes
flags           : fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush dts acpi mmx fxsr sse sse2 ss ht tm pbe syscall nx pdpe1gb rdtscp lm constant_tsc art arch_perfmon pebs bts rep_good nopl xtopology nonstop_tsc cpuid aperfmperf pni pclmulqdq dtes64 monitor ds_cpl vmx est tm2 ssse3 sdbg fma cx16 xtpr pdcm pcid sse4_1 sse4_2 x2apic movbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand lahf_lm abm 3dnowprefetch cpuid_fault epb ssbd ibrs ibpb stibp ibrs_enhanced tpr_shadow flexpriority ept vpid ept_ad fsgsbase tsc_adjust sgx bmi1 avx2 smep bmi2 erms invpcid mpx rdseed adx smap clflushopt intel_pt xsaveopt xsavec xgetbv1 xsaves dtherm ida arat pln pts hwp hwp_notify hwp_act_window hwp_epp vnmi md_clear flush_l1d arch_capabilities
vmx flags       : vnmi preemption_timer invvpid ept_x_only ept_ad ept_1gb flexpriority tsc_offset vtpr mtf vapic ept vpid unrestricted_guest ple pml ept_violation_ve ept_mode_based_exec
bugs            : spectre_v1 spectre_v2 spec_store_bypass swapgs itlb_multihit mmio_stale_data retbleed eibrs_pbrsb bhi its vmscape
bogomips        : 3199.92
clflush size    : 64
cache_alignment : 64
address sizes   : 39 bits physical, 48 bits virtual
power management:
```

```console
$ fastfetch --logo none
lqhuang@nuc-server
------------------
OS: Debian GNU/Linux 13 (trixie) x86_64
Host: NUC10i7FNK (K61156-302)
Kernel: Linux 6.12.57+deb13-amd64
Uptime: 98 days, 18 hours, 6 mins
Packages: 2413 (dpkg)
Shell: zsh 5.9
Cursor: Adwaita
Terminal: /dev/pts/7
CPU: Intel(R) Core(TM) i7-10710U (12) @ 4.70 GHz
GPU: Intel Comet Lake UHD Graphics @ 1.15 GHz [Integrated]
Memory: 5.75 GiB / 31.07 GiB (19%)
Swap: 0 B / 14.90 GiB (0%)
Disk (/): 24.14 GiB / 457.62 GiB (5%) - xfs
Locale: en_US.UTF-8
```

## Software Suites

```sh-session
$ java -version
openjdk version "25.0.3" 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-25.0.3+9 (build 25.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.3+9 (build 25.0.3+9-LTS, mixed mode, sharing)
```

```sh-session
$ clang++ --version
Debian clang version 19.1.7 (3+b1)
Target: x86_64-pc-linux-gnu
Thread model: posix
InstalledDir: /usr/lib/llvm-19/bin
```
