if [ ! -f "checkstyle-10.21.4-all.jar" ]; then
    wget https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.21.4/checkstyle-10.21.4-all.jar
fi

shopt -s globstar
java -jar checkstyle-10.21.4-all.jar -c /google_checks.xml ./src/**/*.java

overall_exit_code=$?
exit $overall_exit_code
