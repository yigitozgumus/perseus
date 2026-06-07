# Perseus Navigation Library

## Binary Compatibility

This project uses the [Kotlin Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator) to track public API changes.

### How it works

The plugin compares the current public API against a stored `.api` file. Any changes to the public API (additions, removals, modifications) are flagged during the build.

### Setup (already configured)

Binary compatibility validation is enabled via Kotlin Gradle Plugin's built-in `abiValidation()` in both `perseus-core` and `perseus-interop` modules.

### Workflow

1. **Make your changes** to the public API.

2. **Check compatibility**:
   ```
   ./gradlew checkKotlinAbi
   ```

3. **If API changes are intentional**, update the reference dump:
   ```
   ./gradlew updateKotlinAbi
   ```

4. **Commit the updated `.abi/` files** along with your changes.

### Non-public API

Internal classes and functions (in `com.yigitozgumus.perseus.internal` package) are excluded from API validation. If you need to expose something internally across modules, use `@InternalPerseusApi` annotation.

### Strict Kotlin

This project uses `explicitApiWarning()` mode. All public declarations must have explicit visibility modifiers and return types. This will be upgraded to `explicitApi()` (errors) in a future version.
