In progress..

# Fire probability


$p_F = \begin{cases}  = 0.05 \quad if \quad \varphi_{G} \geq 0.15 \\ = 0.01 \quad else \end{cases} \quad$ where

Dependent variable: $p_F \in [0,1]$ is fire probability
Independent variable: $\varphi_{G}$ is fraction of grass cover

# Fire damage

*A fire should, on average, destroy 90 % of shrub cover and 70 % of grass cover. The idea is that this creates the grass-fire positive feedback loop. We can play with the parameters to make it work. I think the easiest is to sample from a uniform distribution of these values $\pm$ 10 %*

# Precipitation dependency of growth

$g = \frac{g^*}{e^{r(P - P^{c})}}$ where

Dependent variable: $g \in [0,1]$ is a growth rate scalar
Independent variable: $P$ is annual precipitation in mm (Expected ecosystem range is 0 - 600 mm)

Constants: 
- $g^* = 1$ is the optimal growth rate
- $P^c$ = 350 mm is the critical precipitation in mm ($g(P^c) = 0.5$ )

Parameter: 
- $r$ is a scaling parameter controlling steepness of curve. *Idea is that once we hit a certain precipitation threshold, grasses will overtake shrubs whereas during drought, shrubs are more resilient.*
	- $r_G = 0.05$ (Grasses)
	- $r_S = 0.005$ (Shrubs)
*



