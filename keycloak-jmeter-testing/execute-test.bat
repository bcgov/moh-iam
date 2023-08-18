
Setlocal EnableDelayedExpansion

SET FILE=%1%
SET THREADS=%2%
SET ENV=%3%
SET RAMP_UP=%4%

IF [%RAMP_UP%] == [] (
SET RAMP_UP=%THREADS%
)

jmeter -n -f -t %FILE%.jmx -l %FILE%.jtl -Jloops=1 -Jthreads=%THREADS% -Jramp_up=%RAMP_UP% -q %ENV%.properties