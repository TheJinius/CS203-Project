#!/bin/bash
# Quick test runner script for CS203 Project
# Usage: ./run-tests.sh [options]

set -e

echo "🧪 CS203 Tariff Project - Test Runner"
echo "======================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running!"
    echo "Please start Docker Desktop and try again."
    exit 1
fi

echo "✅ Docker is running"

# Default: run all tests
TEST_SCOPE="all"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --unit)
            TEST_SCOPE="unit"
            shift
            ;;
        --integration)
            TEST_SCOPE="integration"
            shift
            ;;
        --controller)
            TEST_SCOPE="controller"
            shift
            ;;
        --service)
            TEST_SCOPE="service"
            shift
            ;;
        --repository)
            TEST_SCOPE="repository"
            shift
            ;;
        --coverage)
            COVERAGE=true
            shift
            ;;
        --help)
            echo ""
            echo "Usage: ./run-tests.sh [options]"
            echo ""
            echo "Options:"
            echo "  --unit          Run unit tests only"
            echo "  --integration   Run integration tests only"
            echo "  --controller    Run controller tests only"
            echo "  --service       Run service tests only"
            echo "  --repository    Run repository tests only"
            echo "  --coverage      Generate coverage report and open in browser"
            echo "  --help          Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./run-tests.sh                    # Run all tests"
            echo "  ./run-tests.sh --controller       # Run controller tests only"
            echo "  ./run-tests.sh --coverage         # Run all tests with coverage"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help to see available options"
            exit 1
            ;;
    esac
done

# Set test pattern based on scope
case $TEST_SCOPE in
    unit)
        echo "🎯 Running unit tests..."
        TEST_PATTERN="**/*Test.java"
        ;;
    integration)
        echo "🎯 Running integration tests..."
        TEST_PATTERN="**/*IntegrationTest.java"
        ;;
    controller)
        echo "🎯 Running controller tests..."
        TEST_PATTERN="**/controllers/*Test.java"
        ;;
    service)
        echo "🎯 Running service tests..."
        TEST_PATTERN="**/services/*Test.java"
        ;;
    repository)
        echo "🎯 Running repository tests..."
        TEST_PATTERN="**/repositories/**/*Test.java"
        ;;
    all)
        echo "🎯 Running all tests..."
        TEST_PATTERN="**/*Test.java"
        ;;
esac

# Run tests
echo ""
echo "📦 Building and running tests..."
echo ""

if [ "$TEST_SCOPE" == "all" ]; then
    mvn clean test -Dspring.profiles.active=test
else
    mvn test -Dtest="$TEST_PATTERN" -Dspring.profiles.active=test
fi

TEST_EXIT_CODE=$?

# Generate coverage report if requested
if [ "$COVERAGE" == "true" ]; then
    echo ""
    echo "📊 Generating coverage report..."
    mvn jacoco:report
    
    # Open coverage report in browser
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        open target/site/jacoco/index.html
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        xdg-open target/site/jacoco/index.html 2>/dev/null || echo "Coverage report: target/site/jacoco/index.html"
    elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
        # Windows
        start target/site/jacoco/index.html
    else
        echo "Coverage report available at: target/site/jacoco/index.html"
    fi
fi

# Print summary
echo ""
echo "======================================"
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo "✅ All tests passed!"
else
    echo "❌ Some tests failed!"
    echo "Check the output above for details"
fi
echo ""
echo "📝 Test reports: target/surefire-reports/"
if [ "$COVERAGE" == "true" ]; then
    echo "📊 Coverage report: target/site/jacoco/index.html"
fi
echo "======================================"

exit $TEST_EXIT_CODE
