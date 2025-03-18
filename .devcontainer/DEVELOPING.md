## Validating `.josh` Model Configurations

### Example validation

In CI/CD, in order to ensure that our langauge parser is working correctly. Under the hood, this is accomplished by compiling the `java` codebase and validating that the `.josh` configurations are correctly parsed by the language parser. If we break something on the `java` side, the validation will fail, because we assume each of these examples are correct (and will update them if we change the language specification).

We provide a convenience script to validate `.josh` configurations. This script is located at `josh_validator.sh` in the root of the repository.

```bash
$ josh_validator.sh examples/sample.josh 
Validating: examples/sample.josh
Validated Josh code at examples/sample.josh
Validation successful
```

Running the validator on an syntactically incorrect `.josh` file will result in an error message.

```bash
$ josh_validator.sh examples/error.josh 
Validating: examples/error.josh
Found errors in Josh code at examples/error.josh:
 - On line 1: no viable alternative at input 'start test'
Validation failed (exit code: 3)
```
