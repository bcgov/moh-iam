@echo off

SET RESULTS_FILE=%1%
SET REPORT_DIRECTORY=%2%

jmeter -f -g %RESULTS_FILE% -o %REPORT_DIRECTORY%

