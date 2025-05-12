# Succession Model
A simple succession model where growth of two species of tree is influenced by temperature and precipitation which also exhibits extreme heat disturbance. This is a toy model meant to demonstrate Josh language features and not meant to be ecologically complete or validated.

<br>

## Density
Density is represented by agents where each agent may represent a certain number of trees. Each tree species can reach up to 10 agents per patch. The probability of a new agent being created is defined by

$p_c = min(-(n - 5)^2 / 20 + 1.1, 1)$

This parabolic function is limited to 1 and a tree is viable so long as bioclimatic limits are not exceeded. Note that $n$ refers to the total number of trees on the patch.

<br>

## Growth
Growth is a function of both temperature and precipitation. 

### Optimal growth rate
Under optimal conditions, both trees see an optimal growth rate of $g^* = 1$.

### Temperature-dependency of growth
The influence of temperature on growth rate is defined as follows:

$g = g^* - b(T - T^*)^2$

In this sytem:

  - Dependent variable: $g \in [0,1]$ is a growth rate scalar
  - Independent variable: $T$ is temperature in K

Note that $b = 0.0025$ is a scaling parameter and this behavior is subject to the following where $T^*$ is optimal temperature in K:

  - $T_A^* = 300$ (Species A)
  - $T_B^* = 320$ (Species B)

### Precipitation-dependency of growth
The influence of precipitation on growth rate is defined as follows:

$g = \frac{g^*}{e^{r(P - P^{c})}}$

In this system:

  - Dependent variable: $g \in [0,1]$ is a growth rate scalar
  - Independent variable: $P$ is annual precipitation in mm

Note that $r = 0.001$ is a scaling parameter and this behavior is subjec to $P^c$ (the critical precipitation in mm where $g(P^c) = 0.5$ ):

  - $P_A^c = 500$ (Species A)
  - $P_B^c = 400$ (Species B)

<br>

## Survival probability
The probabiliy of survival is defined both under presence and absence of extreme heat stress.

### Typical surivival probability
Where $p_S^*$ is optimal survival probability:

 - $p_{S,A}^* = 0.3$ (Species A)
 - $p_{S,B}^* = 0.5$ (Species B)

These probabilities may be modified under extreme heat stress.

### Bioclimatic limits
Probablity of survival ($p_S$) is limited by extreme heat as described by the following in K:

 - $= 0 \quad if \quad T < 273  \quad or \quad T > 373$
 - $= p_S^* else$
 
In this system, consider the following:

 - Dependent variable: $p_S \in [0,1]$ is survival probability
 - Independent variable: $T$ is temperature in K

These stresses are read from external data.

<br>

## Demo
To demonstrate use of this toy simulation, the following 100 meter resolution example is suggested for use:

```
grid.size = 100 m
grid.low = 36.52 degrees latitude, -118.68 degrees longitude
grid.high = 36.42 degrees latitude, -118.45 degrees longitude

steps.low = 0 count
steps.high = 20 count
```

This uses [Cal-Adapt](https://cal-adapt.org/) data.
