# Josh Output System Architecture

This document describes the unified OutputWriter architecture used for both debug and export output in Josh simulations.

## Overview

Josh uses a unified, generic output system based on the `OutputWriter<T>` abstraction. This replaces the previous separate debug and export facade systems with a single, consistent architecture that eliminates code duplication and provides uniform behavior across all output types.

## Core Abstraction

### OutputWriter<T> Interface

The foundation of the system is the generic `OutputWriter<T>` interface:

```java
public interface OutputWriter<T> {
    void write(T data, long step);
    void start();
    void join();
    String getPath();
}
```

**Type Parameter**:
- `T`: The type of data being written
  - `String` for debug output (plain text messages)
  - `DataRow` for export output (structured CSV data)

**Lifecycle**:
1. `start()` - Initialize the writer (open files, start background threads)
2. `write(data, step)` - Queue data for asynchronous writing
3. `join()` - Wait for all pending writes to complete and close resources

### OutputTarget Model

`OutputTarget` represents a destination for output data:

```java
public class OutputTarget {
    private final String protocol;    // file, minio, stdout, memory
    private final String path;         // destination path
    private final String fileType;     // txt, csv, etc.
}
```

**Supported Protocols**:
- `file://` - Local filesystem (e.g., `file:///tmp/debug.txt`)
- `minio://` - MinIO/S3 storage (e.g., `minio://bucket/path/file.txt`)
- `stdout://` - Standard output (e.g., `stdout`)
- `memory://` - In-memory storage for web editor (e.g., `memory://editor/debug`)

**Template Variables**:
Output paths support template variables for organizing output:
- `{replicate}` - Replicate number (0, 1, 2, ...)
- `{user}` - Custom tag for user identification
- `{editor}` - Custom tag for editor/configuration name
- Any custom tags passed via `--custom-tag` CLI argument

Example: `file:///tmp/{user}/{editor}/debug_{replicate}.txt`

## Type-Specific Implementations

### TextOutputWriter (Debug Output)

`TextOutputWriter` writes plain text strings for debug messages:

```java
public class TextOutputWriter implements OutputWriter<String> {
    private final OutputTarget target;
    private final OutputStreamStrategy streamStrategy;
    private final BlockingQueue<WriteTask<String>> queue;

    @Override
    public void write(String message, long step) {
        // Format: [Step X, entityType] "message content"
        queue.offer(new WriteTask<>(message, step));
    }
}
```

**Features**:
- Asynchronous writing via background thread and queue
- Automatic message formatting with step number and entity type
- Zero overhead when not configured (queue and thread created on demand)
- Thread-safe for parallel simulations

**Output Format**:
```
[Step 0, organism] "Tree created at" "(34.25, -116.25, 0)"
[Step 1, organism] "Step" "1" "age:" "1 years" "height:" "1.5 meters"
```

### StructuredOutputWriter (Export Output)

`StructuredOutputWriter` writes structured CSV data for export:

```java
public class StructuredOutputWriter implements OutputWriter<DataRow> {
    private final OutputTarget target;
    private final OutputStreamStrategy streamStrategy;
    private final BlockingQueue<WriteTask<DataRow>> queue;

    @Override
    public void write(DataRow row, long step) {
        // Writes CSV row with proper quoting and escaping
        queue.offer(new WriteTask<>(row, step));
    }
}
```

**Features**:
- CSV format with proper quoting and escaping
- Header row automatically written
- Step and replicate columns as last two columns
- Compatible with existing export format

**Output Format**:
```csv
type,geoKey,age,generation,step,replicate
Tree,"(34.1, -116.3, 0)",5,1,10,0
```

## Combined Writers (Per-Entity Routing)

### CombinedTextWriter

Routes debug messages to different writers based on entity type:

```java
public class CombinedTextWriter implements OutputWriter<String> {
    private final Map<String, OutputWriter<String>> writersByEntityType;
    private final ThreadLocal<String> currentEntityType;

    public void setCurrentEntityType(String entityType) {
        currentEntityType.set(entityType);
    }

    @Override
    public void write(String message, long step) {
        String entityType = currentEntityType.get();
        OutputWriter<String> writer = writersByEntityType.get(entityType);
        if (writer != null) {
            writer.write(message, step);
        }
    }
}
```

**Entity Types**:
- `patch` - For patch-level debug output
- `organism` - For organism-level debug output
- `agent` - For agent-level debug output

**Configuration Example**:
```josh
debugFiles.patch = "file:///tmp/patch_debug.txt"
debugFiles.organism = "file:///tmp/organism_debug.txt"
debugFiles.agent = "stdout"
```

### CombinedStructuredWriter

Similar routing for export output, routing structured data rows to different CSV files based on entity type.

## Factory Pattern

### OutputWriterFactory

Centralized factory for creating all output writers:

```java
public class OutputWriterFactory {
    private final int replicate;
    private final PathTemplateResolver templateResolver;
    private final MinioOptions minioOptions;

    public OutputWriter<String> createTextWriter(String targetUri) {
        OutputTarget target = parseTarget(targetUri);
        OutputStreamStrategy strategy = createStrategy(target);
        return new TextOutputWriter(target, strategy);
    }

    public OutputWriter<DataRow> createStructuredWriter(String targetUri) {
        OutputTarget target = parseTarget(targetUri);
        OutputStreamStrategy strategy = createStrategy(target);
        return new StructuredOutputWriter(target, strategy);
    }
}
```

**Responsibilities**:
- Parse output URIs into `OutputTarget` objects
- Resolve template variables in paths
- Create appropriate `OutputStreamStrategy` for each protocol
- Configure MinIO credentials when needed

## Stream Strategies

Different strategies handle different output protocols:

### FileOutputStreamStrategy
- Opens local files for writing
- Creates parent directories as needed
- Handles file permissions

### MinioOutputStreamStrategy
- Connects to MinIO/S3 endpoints
- Handles authentication and bucket operations
- Streams data to cloud storage

### StdoutOutputStreamStrategy
- Writes to standard output
- Flushes after each write for immediate visibility

### MemoryOutputStreamStrategy
- Stores output in memory for web editor
- Provides access to buffered content

## Queue-Based Async Writing

All writers use a queue-based asynchronous architecture:

```java
public class WriteTask<T> {
    private final T data;
    private final long step;
    private final String entityType;
    private final int replicateNumber;
}
```

**Benefits**:
- Non-blocking writes - simulation doesn't wait for I/O
- Ordered writes - FIFO queue preserves message order
- Thread-safe - lock-free queue operations
- Backpressure - bounded queue prevents memory issues

**Background Thread**:
Each writer spawns a background thread that:
1. Polls the queue for new tasks
2. Formats the data appropriately (text or CSV)
3. Writes to the output stream
4. Handles errors and retries
5. Terminates on shutdown signal

## Integration with Josh

### JvmInputOutputLayer

The main I/O layer creates and manages the OutputWriterFactory:

```java
public class JvmInputOutputLayer implements InputOutputLayer {
    private final OutputWriterFactory outputWriterFactory;

    public JvmInputOutputLayer(OutputWriterFactory factory, ...) {
        this.outputWriterFactory = factory;
    }

    @Override
    public OutputWriterFactory getOutputWriterFactory() {
        return outputWriterFactory;
    }
}
```

### Debug Function Integration

The `debug()` function in Josh scripts uses the output writer:

```java
public EventHandlerMachine writeDebug(String message) {
    if (debugWriter.isEmpty()) {
        return this; // Zero overhead when not configured
    }

    String entityCategory = determineEntityType(); // "patch", "organism", "agent"
    long step = bridge.getCurrentTimestep();

    CombinedTextWriter writer = debugWriter.get();
    writer.setCurrentEntityType(entityCategory);
    writer.write(message, step);

    return this;
}
```

### Export Configuration

Export output is configured similarly through simulation attributes:

```josh
exportFiles.agent = "file:///tmp/organisms.csv"
exportFiles.patch = "file:///tmp/patches.csv"
```

## Design Patterns

### Generic Interface Pattern
- Single `OutputWriter<T>` interface serves both text and structured output
- Type parameter ensures type safety
- Eliminates code duplication between debug and export systems

### Factory Pattern
- Centralized creation logic in `OutputWriterFactory`
- Encapsulates protocol detection and strategy selection
- Simplifies testing and mocking

### Strategy Pattern
- Different `OutputStreamStrategy` implementations for each protocol
- Easy to add new output destinations
- Protocol-specific logic isolated in strategy classes

### Adapter Pattern
- `CombinedDebugOutputWriter` adapts old facade API to new writer system
- Allows gradual migration of calling code
- Maintains backward compatibility during transition

### ThreadLocal Context Pattern
- Entity type stored in `ThreadLocal` for thread-safe routing
- Zero overhead when not used
- Supports parallel simulation execution

## Migration from Old System

### Old Architecture (Deprecated, Now Deleted)

Previously, Josh had separate facade systems:
- **Debug**: `DebugFacade`, `CombinedDebugFacade`, `DebugFacadeFactory`
- **Export**: `ExportFacade`, `CombinedExportFacade`, `ExportFacadeFactory`

Each system had its own:
- File/MinIO/stdout/memory implementations
- Factory classes (JVM and sandbox variants)
- Queue and task structures
- Configuration parsing

This resulted in ~60% code duplication.

### New Architecture (Current)

Unified system with:
- Single `OutputWriter<T>` abstraction
- Shared `OutputTarget` and `WriteTask<T>` models
- Single `OutputWriterFactory` for all output types
- Shared stream strategies for all protocols

**Benefits**:
- 40% reduction in output-related code
- Consistent behavior across debug and export
- Easier to test and maintain
- Single source of truth for output handling

### Phase 5 Cleanup (Completed 2025-11-08)

Old debug facade files deleted:
- `DebugFacade.java`
- `CombinedDebugFacade.java`
- `DebugFacadeFactory.java`
- `JvmDebugFacadeFactory.java`
- `SandboxDebugFacadeFactory.java`
- `FileDebugFacade.java`
- `MinioDebugFacade.java`
- `StdoutDebugFacade.java`
- `MemoryDebugFacade.java`
- `DebugTask.java`

Interpreter updated to use `CombinedTextWriter` instead of `CombinedDebugFacade`:
- `BridgeGetter` interface
- `FutureBridgeGetter` implementation
- `SingleThreadEventHandlerMachine`
- `PushDownMachineCallable`

## Performance Characteristics

### Zero Overhead When Disabled
- If no debug/export files configured, writers are never created
- No queue allocation, no thread spawning
- No performance impact on simulations without output

### Asynchronous Writing
- Simulation thread doesn't block on I/O
- Background thread handles all disk/network operations
- Queue-based buffering handles I/O speed variations

### Memory Efficiency
- Bounded queues prevent unbounded memory growth
- Backpressure mechanism when queue fills
- Strings/DataRows garbage collected after writing

### Scalability
- ThreadLocal context allows parallel simulation threads
- Each replicate has independent writers and queues
- No contention between parallel executions

## Future Enhancements

### Potential Additions
- **JSON Output**: Add `JsonOutputWriter implements OutputWriter<JsonNode>`
- **Parquet Output**: Add `ParquetOutputWriter implements OutputWriter<DataRow>`
- **Database Output**: Add `DatabaseOutputWriter` with JDBC strategy
- **Compression**: Add gzip/zstd compression in stream strategies

### Extension Points
- New protocols: Implement new `OutputStreamStrategy`
- New formats: Implement new `OutputWriter<T>` for type T
- New routing: Extend `CombinedTextWriter` routing logic
- New templates: Add template variables to `PathTemplateResolver`

## Related Documentation

- **User Guide**: See `DEBUG_QUICKSTART.md` for usage examples
- **Task Documentation**: See `.claude/tasks/unify-debug-export-output-systems.md` for implementation details
- **API Documentation**: See JavaDoc in source files

## Questions?

For issues or questions about the output system:
1. Check this architecture document
2. Review DEBUG_QUICKSTART.md for usage examples
3. See JavaDoc in OutputWriter.java and related classes
4. Check implementation task: `.claude/tasks/unify-debug-export-output-systems.md`

---

Last updated: 2025-11-08 (Phase 5 completion)
