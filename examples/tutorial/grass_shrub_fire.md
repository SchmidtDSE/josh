# Grass Fire Cycle
Simple model showing disturbances based on simulation conditions with external data. This shows grass and shrub growth which increases probability of fires.

## Vegitation
Vegitation grows according to a growth rate which is influenced by precipitation.

### Growth and precipitation
$g = \frac{g^*}{e^{r(P - P^{c})}}$

Put more formally:

- Dependent variable: $g \in [0,1]$ is a growth rate scalar
- Independent variable: $P$ is annual precipitation in mm (Expected ecosystem range is 0 - 600 mm)

These contants define the behavior: 

- $g^* = 1$ is the optimal growth rate
- $P^c$ = 350 mm is the critical precipitation in mm ($g(P^c) = 0.5$ )

### Species parameters
In these growth formulas, $r$ is a scaling parameter controlling steepness of curve. The idea is that once we hit a certain precipitation threshold, grasses will overtake shrubs whereas during drought, shrubs are more resilient. This is defined as such:

- $r_G = 0.05$ (Grasses)
- $r_S = 0.005$ (Shrubs)

Both kinds of vegitation are influenced by fire.

## Fire
Fire acts as the distrurbance in this system which happens probabilistically.

### Probability
The probability of fire is a function of grass cover where probability per time-step is as follows. $p_F =$

- $0.05 \quad if \quad \varphi_{G} \geq 0.15$
- $0.01 \quad else \quad$

More formally,

- Dependent variable: $p_F \in [0,1]$ is fire probability
- Independent variable: $\varphi_{G}$ is fraction of grass cover

### Damage
A fire should, on average, destroy 90% of shrub cover and 70% of grass cover. The idea is that this creates the grass-fire positive feedback loop. These are defined by sampling probailities directly when a patch is on fire.
