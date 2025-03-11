# Technologies

Will use JUnit, JavaDoc, Decimal, and [Gradle](https://github.com/junit-team/junit5-samples/blob/r5.12.0/junit5-jupiter-starter-gradle/build.gradle) which, at the start, only needs to pull in JUnit via Maven (MavenCentral). Will live under the `org.dse.JoshLang`.

# Engine API
Each subheader is an object.

## EngineValue

Arithmetic operations will throw an error if units are not the same.

`add(EngineValue other) -> EngineValue`

`subtract(EngineValue other) -> EngineValue`

`multiply(EngineValue other) -> EngineValue`

`divide(EngineValue other) -> EngineValue`

`raiseToPower(EngineValue other) -> EngineValue`

`getUnits() -> String`

## Distribution(EngineValue)

`sample() -> Scalar`

`getSize() -> Optional<Integer>`

`getContents(int count, boolean withReplacement) -> Iterable<EngineValue>`

## RealizedDistribution(Distribution)

## VirtualDistribution(Distribution)

### StandardVirtualDistrubtion(Distribution)

## Set(Distribution)

## Scalar(EngineValue)

## ConverterBuilder

`addConversion(Conversion conversion)`

`build() -> Converter`

## Converter

`getConversion(String oldUnits, String newUnits) -> Conversion`

## Conversion

`getSourceUnits() -> string`

`getDestinationUnits() -> string`

`getConversionCallable() -> CompiledCallable`

## EntityBuilder

`addEventHandlers(EventHandlerGroup group)`

`build() -> Entity`

## Entity

`getFromPriorStep() -> Optional<Entity>`

Get a copy of this entity from the prior time step or an empty java.util.Optional if this entity did not exist in the prior timestep or no prior timestep exists.

`getEventHandler(String attribute, String event) -> Iterable<EventHandlerGroup>`

`getAttributeValue(String name) -> Optional<EngineValue>`

`setAttributeValue(String name, EngineValue value)`

## ExternalResource(Entity)

`getValues(Geometry geometry) -> Distribution`

## Simulation(Entity)

## SpatialEntity

`getLocation() -> GeoPoint`

## Patch(SpatialEntity)

## Agent(SpatialEntity)

## Distrurbance(SpatialEntity)

## EventHandler

`getAttributeName() -> string`

`getEventName() -> string`

`getConditional() -> Optional<CompiledSelector>`

## EventHandlerGroup

`getEventHandlers() -> Iterable<EventHandler>`

`getState() -> Optional<String>`

## CompiledSelector

`evaluate(Scope scope) -> bool`

## CompiledCallable

`evaluate(Scope scope) -> EngineValue`

## Geometry

`getCenterLatitude() -> Decimal`

`getCenterLongitude() -> Decimal`

`getRadius() -> Decimal`

## GeoPoint(Geometry)

## Circle(Geometry)

## Query

`getGeometry() -> Geometry`

`getStep() -> int`

## ReplicateBuilder

`addEntity(EntityBuilder entity)`

`step() -> Replicate`

Take a step where all entities do not have a prior state.

`step(Replicate currentReplicate) -> Replicate`

Advance to next step where the curent step is reprsented by `currentReplicate`.

## TimeStep

`getTimeStep() -> int`

`getEntities(Geometery geometry) -> Iterable<Entity>`

`getEntities(Geometery geometry, String name) -> Iterablel<Entity>`

`getEntities() -> Iterable<Entity>`

## Replicate

`addTimeStep(TimeStep timeStep)`

`getTimeStep(int stepNumber) -> Optional<TimeStep>`

`query(Query query) -> Iterable<Entity>`

## ConfigBuilder

`addValue(String name, EngineValue value)`

`build() -> Config`

## Config

`getValue(String name) -> EngineValue`


# Results format

Avro for now.
