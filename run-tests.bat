@echo off
REM Quick test runner script for CS203 Project (Windows)
REM Usage: run-tests.bat [options]

setlocal enabledelayedexpansion

echo.
echo CS203 Tariff Project - Test Runner
echo ======================================

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not running!
    echo Please start Docker Desktop and try again.
    exit /b 1
)

echo Docker is running

REM Default: run all tests
set "TEST_SCOPE=all"
set "COVERAGE="

REM Parse arguments
:parse_args
if "%1"=="" goto run_tests
if /i "%1"=="--unit" (
    set "TEST_SCOPE=unit"
    shift
    goto parse_args
)
if /i "%1"=="--integration" (
    set "TEST_SCOPE=integration"
    shift
    goto parse_args
)
if /i "%1"=="--controller" (
    set "TEST_SCOPE=controller"
    shift
    goto parse_args
)
if /i "%1"=="--service" (
    set "TEST_SCOPE=service"
    shift
    goto parse_args
)
if /i "%1"=="--repository" (
    set "TEST_SCOPE=repository"
    shift
    goto parse_args
)
if /i "%1"=="--coverage" (
    set "COVERAGE=true"
    shift
    goto parse_args
)
if /i "%1"=="--help" (
    echo.
    echo Usage: run-tests.bat [options]
    echo.
    echo Options:
    echo   --unit          Run unit tests only
    echo   --integration   Run integration tests only
    echo   --controller    Run controller tests only
    echo   --service       Run service tests only
    echo   --repository    Run repository tests only
    echo   --coverage      Generate coverage report and open in browser
    echo   --help          Show this help message
    echo.
    echo Examples:
    echo   run-tests.bat                    # Run all tests
    echo   run-tests.bat --controller       # Run controller tests only
    echo   run-tests.bat --coverage         # Run all tests with coverage
    exit /b 0
)
echo Unknown option: %1
echo Use --help to see available options
exit /b 1

:run_tests
REM Set test pattern based on scope
if "%TEST_SCOPE%"=="unit" (
    echo Running unit tests...
    set "TEST_PATTERN=**/*Test.java"
) else if "%TEST_SCOPE%"=="integration" (
    echo Running integration tests...
    set "TEST_PATTERN=**/*IntegrationTest.java"
) else if "%TEST_SCOPE%"=="controller" (
    echo Running controller tests...
    set "TEST_PATTERN=**/controllers/*Test.java"
) else if "%TEST_SCOPE%"=="service" (
    echo Running service tests...
    set "TEST_PATTERN=**/services/*Test.java"
) else if "%TEST_SCOPE%"=="repository" (
    echo Running repository tests...
    set "TEST_PATTERN=**/repositories/**/*Test.java"
) else (
    echo Running all tests...
    set "TEST_PATTERN=**/*Test.java"
)

echo.
echo Building and running tests...
echo.

REM Run tests
if "%TEST_SCOPE%"=="all" (
    call mvn clean test -Dspring.profiles.active=test
) else (
    call mvn test -Dtest="%TEST_PATTERN%" -Dspring.profiles.active=test
)

set TEST_EXIT_CODE=%errorlevel%

REM Generate coverage report if requested
if "%COVERAGE%"=="true" (
    echo.
    echo Generating coverage report...
    call mvn jacoco:report
    
    REM Open coverage report in browser
    start target\site\jacoco\index.html
)

REM Print summary
echo.
echo ======================================
if %TEST_EXIT_CODE% equ 0 (
    echo All tests passed!
) else (
    echo Some tests failed!
    echo Check the output above for details
)
echo.
echo Test reports: target\surefire-reports\
if "%COVERAGE%"=="true" (
    echo Coverage report: target\site\jacoco\index.html
)
echo ======================================

exit /b %TEST_EXIT_CODE%
