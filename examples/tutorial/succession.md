In progress..

  
# Bioclimatic limits

$p_S = \begin{cases}  = 0 \quad if \quad T < 273  \quad or \quad T > 373 \\ = p_S^* \quad else \end{cases} \quad$ where

Dependent variable: $p_S \in [0,1]$ is survival probability
Independent variable: $T$ is temperature in K

Parameter: 
- $p_S^*$ is optimal survival probability
	- $p_{S,A}^* = 0.3$ (Species A)
	- $p_{S,B}^* = 0.5$ (Species B)

# Temperature dependency of growth

$g = g^* - b(T - T^*)^2$  where

Dependent variable: $g \in [0,1]$ is a growth rate scalar
Independent variable: $T$ is temperature in K

Constants: 
- $g^* = 1$ is the optimal growth rate
- $b = 0.0025$ is a scaling parameter
Parameter: 
- $T^*$ is optimal temperature in K
	- $T_A^* = 300$ (Species A)
	- $T_B^* = 320$ (Species B)

# Precipitation dependency of growth

plot y = 1/(1 + e^(-0.01(x - 500) ))

$g = \frac{g^*}{e^{r(P - P^{c})}}$ where

Dependent variable: $g \in [0,1]$ is a growth rate scalar
Independent variable: $P$ is annual precipitation in mm

Constants: 
- $g^* = 1$ is the optimal growth rate
- $r = 0.001$ is a scaling parameter
Parameter: 
- $P^c$ is the critical precipitation in mm ( $g(P^c) = 0.5$ )
	- $P_A^c = 500$ (Species A)
	- $P_B^c = 400$ (Species B)
