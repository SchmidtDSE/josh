package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.engine.config.Config;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.precompute.DataGridLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MinimalEngineBridgeConcurrencyTest {

  private static final int THREAD_COUNT = 16;

  @Mock(lenient = true) private MutableEntity mockSimulation;
  @Mock(lenient = true) private Replicate mockReplicate;
  @Mock(lenient = true) private Converter mockConverter;
  @Mock(lenient = true) private EntityPrototypeStore mockPrototypeStore;

  private EngineValueFactory engineValueFactory;

  @BeforeEach
  void setUp() {
    engineValueFactory = new EngineValueFactory();
  }

  @Test
  void getExternalConcurrentAccessShouldNotThrowNpe() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);
    DataGridLayer mockLayer = mock(DataGridLayer.class);
    GeoKey mockKey = mock(GeoKey.class);
    EngineValue mockValue = engineValueFactory.build(1.0, Units.of("mm"));

    when(mockLayer.getAt(mockKey, 0L)).thenReturn(mockValue);

    ExternalResourceGetter externalGetter = name -> {
      callCount.incrementAndGet();
      try {
        Thread.sleep(10); // simulate slow I/O
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return mockLayer;
    };

    MinimalEngineBridge bridge = new MinimalEngineBridge(
        engineValueFactory,
        new GridGeometryFactory(),
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        externalGetter,
        new org.joshsim.engine.config.NoOpConfigGetter(),
        mockReplicate
    );

    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<EngineValue>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return bridge.getExternal(mockKey, "Precipitation", 0L);
      }));
    }

    for (Future<EngineValue> future : futures) {
      EngineValue result = future.get();
      assertNotNull(result, "getExternal should never return null under concurrency");
    }

    assertEquals(1, callCount.get(),
        "getResource should be called exactly once via computeIfAbsent");

    executor.shutdown();
  }

  @Test
  void getConfigOptionalConcurrentAccessShouldNotThrowNpe() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);
    Config mockConfig = mock(Config.class);
    EngineValue mockValue = engineValueFactory.build(42.0, Units.of("meters"));
    when(mockConfig.getValue("testVar")).thenReturn(mockValue);

    ConfigGetter configGetter = name -> {
      callCount.incrementAndGet();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return Optional.of(mockConfig);
    };

    MinimalEngineBridge bridge = new MinimalEngineBridge(
        engineValueFactory,
        new GridGeometryFactory(),
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        mock(ExternalResourceGetter.class),
        configGetter,
        mockReplicate
    );

    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<Optional<EngineValue>>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return bridge.getConfigOptional("test.testVar");
      }));
    }

    for (Future<Optional<EngineValue>> future : futures) {
      Optional<EngineValue> result = future.get();
      assertNotNull(result, "getConfigOptional should never return null under concurrency");
    }

    assertEquals(1, callCount.get(),
        "getConfig should be called exactly once via computeIfAbsent");

    executor.shutdown();
  }

  @Test
  void queryCacheBridgeConcurrentPriorPatchesShouldNotThrowNpe() throws Exception {
    GeoKey mockKey = mock(GeoKey.class);
    MutableEntity mockPatch = mock(MutableEntity.class);
    when(mockPatch.getKey()).thenReturn(Optional.of(mockKey));

    org.joshsim.engine.geometry.EngineGeometry mockGeometry = mock(
        org.joshsim.engine.geometry.EngineGeometry.class);
    GeometryMomento mockMomento = mock(GeometryMomento.class);
    when(mockMomento.build()).thenReturn(mockGeometry);

    when(mockReplicate.query(
        org.mockito.ArgumentMatchers.any(org.joshsim.engine.simulation.Query.class)))
        .thenReturn(List.of(mockPatch));
    when(mockReplicate.getPatchByKey(mockKey, -1L)).thenReturn(mockPatch);

    QueryCacheEngineBridge bridge = new QueryCacheEngineBridge(
        engineValueFactory,
        new GridGeometryFactory(),
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        mock(ExternalResourceGetter.class),
        new org.joshsim.engine.config.NoOpConfigGetter(),
        mockReplicate
    );

    CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<List<org.joshsim.engine.entity.base.Entity>>> futures = new ArrayList<>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return bridge.getPriorPatches(mockMomento);
      }));
    }

    for (Future<List<org.joshsim.engine.entity.base.Entity>> future : futures) {
      List<org.joshsim.engine.entity.base.Entity> result = future.get();
      assertNotNull(result, "getPriorPatches should never return null under concurrency");
    }

    executor.shutdown();
  }
}
