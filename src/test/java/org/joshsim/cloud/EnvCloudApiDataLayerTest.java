package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class EnvCloudApiDataLayerTest {

  @Test
  void apiKeyIsValid_shouldReturnTrue_whenEnvVariableIsNull() {
    // Arrange
    EnvCloudApiDataLayer.ApiKeyStringGetter mockApiKeyGetter = Mockito.mock(
        EnvCloudApiDataLayer.ApiKeyStringGetter.class
    );
    Mockito.when(mockApiKeyGetter.getApiKeysString()).thenReturn(null);
    EnvCloudApiDataLayer dataLayer = new EnvCloudApiDataLayer(mockApiKeyGetter);

    // Act
    boolean result = dataLayer.apiKeyIsValid("any_key");

    // Assert
    assertTrue(result);
  }

  @Test
  void apiKeyIsValid_shouldReturnTrue_whenEnvVariableIsEmpty() {
    // Arrange
    EnvCloudApiDataLayer.ApiKeyStringGetter mockApiKeyGetter = Mockito.mock(
        EnvCloudApiDataLayer.ApiKeyStringGetter.class
    );
    Mockito.when(mockApiKeyGetter.getApiKeysString()).thenReturn("");
    EnvCloudApiDataLayer dataLayer = new EnvCloudApiDataLayer(mockApiKeyGetter);

    // Act
    boolean result = dataLayer.apiKeyIsValid("any_key");

    // Assert
    assertTrue(result);
  }

  @Test
  void apiKeyIsValid_shouldReturnTrue_whenKeyExistsInEnvVariable() {
    // Arrange
    String validKey = "valid_key";
    EnvCloudApiDataLayer.ApiKeyStringGetter mockApiKeyGetter = Mockito.mock(
        EnvCloudApiDataLayer.ApiKeyStringGetter.class
    );
    Mockito.when(mockApiKeyGetter.getApiKeysString()).thenReturn("valid_key,other_key");
    EnvCloudApiDataLayer dataLayer = new EnvCloudApiDataLayer(mockApiKeyGetter);

    // Act
    boolean result = dataLayer.apiKeyIsValid(validKey);

    // Assert
    assertTrue(result);
  }

  @Test
  void apiKeyIsValid_shouldReturnFalse_whenKeyDoesNotExistInEnvVariable() {
    // Arrange
    String invalidKey = "invalid_key";
    EnvCloudApiDataLayer.ApiKeyStringGetter mockApiKeyGetter = Mockito.mock(
        EnvCloudApiDataLayer.ApiKeyStringGetter.class
    );
    Mockito.when(mockApiKeyGetter.getApiKeysString()).thenReturn("valid_key,other_key");
    EnvCloudApiDataLayer dataLayer = new EnvCloudApiDataLayer(mockApiKeyGetter);

    // Act
    boolean result = dataLayer.apiKeyIsValid(invalidKey);

    // Assert
    assertFalse(result);
  }

  @Test
  void apiKeyIsValid_shouldIgnoreWhitespaceAroundKeys() {
    // Arrange
    String validKey = "valid_key";
    EnvCloudApiDataLayer.ApiKeyStringGetter mockApiKeyGetter = Mockito.mock(
        EnvCloudApiDataLayer.ApiKeyStringGetter.class
    );
    Mockito.when(mockApiKeyGetter.getApiKeysString()).thenReturn(" valid_key , other_key ");
    EnvCloudApiDataLayer dataLayer = new EnvCloudApiDataLayer(mockApiKeyGetter);

    // Act
    boolean result = dataLayer.apiKeyIsValid(validKey);

    // Assert
    assertTrue(result);
  }
}