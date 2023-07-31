SET FILE=%1%
SET THREADS=%2%
SET ENV=%3%

jmeter -n -f -t %FILE%.jmx -l %FILE%.jtl -Jloops=1 -Jthreads=%THREADS% -Jramp_up=%THREADS% -q %ENV%.properties