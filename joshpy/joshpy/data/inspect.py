from wasmer import Store, Module, Type


def get_function_details(wasm_path):
    # Load the WASM file
    wasm_bytes = open(wasm_path, 'rb').read()

    # Create module
    store = Store()
    module = Module(store, wasm_bytes)

    # Get export types
    export_types = module.exports

    # Print detailed information about each function export
    print("Function exports:")
    for export in export_types:
        if export.type.kind() == Type.FUNCTION:
            func_type = export.type.function_type()
            param_types = [str(p) for p in func_type.params]
            result_types = [str(r) for r in func_type.results]

            print(f"  - {export.name}:")
            print(f"      Parameters: {param_types}")
            print(f"      Results: {result_types}")

    return module


# Example usage
module = get_function_details('JoshSim.wasm')
