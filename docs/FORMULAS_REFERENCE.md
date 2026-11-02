# Formulaire de Résistance des Matériaux (RDM) - Civil Engineering Reference

Ce document constitue la référence mathématique et physique utilisée par le solveur RDM.

---

## 1. Conventions de Signes (Mécanique des Structures - Eurocode & Tradition Française)

- **Repère :** 
  - Axe longitudinal : $x \in [0, L]$, orienté de gauche à droite.
  - Axe transversal : $y$, orienté vers le haut ($+y \uparrow$).
  - Rotation : sens trigonométrique antihoraire positif ($+ \circlearrowleft$).

- **Sollicitations Internes (Convention de la section à gauche) :**
  Pour une coupure à l'abscisse $x$, les efforts intérieurs équilibrent les actions extérieures situées à gauche de la coupure ($x' \le x$) :
  - **Effort normal $N(x)$ :**
    $$N(x) = - \sum F_{x,\text{gauche}}$$
    - $N(x) > 0$ : Traction (tension).
    - $N(x) < 0$ : Compression.
  - **Effort tranchant $T(x)$ (ou $V(x)$) :**
    $$T(x) = \sum F_{y,\text{gauche}}$$
    - Une force ascendante à gauche produit un effort tranchant positif.
  - **Moment fléchissant $M(x)$ :**
    $$M(x) = \sum M_{(x),\text{gauche}} = \sum \left[ F_{y,i} \cdot (x - x_i) - C_i \right]$$
    - $M(x) > 0$ : Fibres inférieures tendues (moment positif, "fléchissement en cuvette / souriant").
    - Relations différentielles fondamentales :
      $$\frac{dM(x)}{dx} = T(x)$$
      $$\frac{dT(x)}{dx} = -q(x)$$
      $$\frac{d^2M(x)}{dx^2} = -q(x)$$

---

## 2. Équilibre Statique Global (Phase 1)

Pour une poutre isostatique dans le plan $(x, y)$ :
$$\sum F_x = 0 \implies R_{Ax} + \sum F_{x,\text{ext}} = 0$$
$$\sum F_y = 0 \implies R_{Ay} + R_{By} - \sum F_{y,\text{ext}} = 0$$
$$\sum M_{/A} = 0 \implies R_{By} \cdot d_{AB} + \sum M_{A}(F_{\text{ext}}) + \sum C_{\text{ext}} - M_{A,\text{enc}} = 0$$

### Résultante des charges réparties :
1. **Charge uniforme $q$ sur $[x_1, x_2]$ :**
   $$Q = q \cdot (x_2 - x_1)$$
   $$x_G = \frac{x_1 + x_2}{2}$$
2. **Charge triangulaire (de $0$ en $x_1$ à $q$ en $x_2$) :**
   $$Q = \frac{1}{2} q \cdot (x_2 - x_1)$$
   $$x_G = x_1 + \frac{2}{3}(x_2 - x_1)$$
3. **Charge trapézoïdale ($q_1$ en $x_1$ à $q_2$ en $x_2$) :**
   $$Q = \frac{q_1 + q_2}{2} \cdot (x_2 - x_1)$$
   $$x_G = x_1 + \frac{x_2 - x_1}{3} \cdot \frac{q_1 + 2q_2}{q_1 + q_2}$$

---

## 3. Cas Types Incontournables (Validés par les Tests)

| Configuration | Réactions | $T(x)$ | $M(x)$ | $M_{\max}$ |
|---|---|---|---|---|
| **Bi-appuyée + Charge uniforme $q$ sur $L$** | $R_A = R_B = \frac{qL}{2}$ | $T(x) = \frac{qL}{2} - qx$ | $M(x) = \frac{qx}{2}(L - x)$ | $M_{\max} = \frac{qL^2}{8}$ en $x = \frac{L}{2}$ |
| **Bi-appuyée + Force ponctuelle $F$ à mi-travée** | $R_A = R_B = \frac{F}{2}$ | $\pm \frac{F}{2}$ | $M(x) = \frac{Fx}{2}$ ($x \le L/2$) | $M_{\max} = \frac{FL}{4}$ en $x = \frac{L}{2}$ |
| **Bi-appuyée + Force ponctuelle $F$ en $a$ ($b = L-a$)** | $R_A = \frac{Fb}{L}$, $R_B = \frac{Fa}{L}$ | $R_A$ puis $-R_B$ | $R_A \cdot x$ | $M_{\max} = \frac{Fab}{L}$ en $x = a$ |
| **Console encastrée en 0 + Force $F$ à l'extrémité $L$** | $R_{Ay} = F$, $M_A = -FL$ | $T(x) = F$ | $M(x) = -F(L - x)$ | $\|M_{\max}\| = FL$ en $x = 0$ |
| **Console encastrée en 0 + Charge uniforme $q$ sur $L$** | $R_{Ay} = qL$, $M_A = -\frac{qL^2}{2}$ | $T(x) = q(L - x)$ | $M(x) = -\frac{q(L-x)^2}{2}$ | $\|M_{\max}\| = \frac{qL^2}{2}$ en $x = 0$ |

---

## 4. Règles de Vérification Automatique

1. **Équilibre global :** $\|\sum F_x\| < 10^{-4}$ kN, $\|\sum F_y\| < 10^{-4}$ kN, $\|\sum M_{/0}\| < 10^{-4}$ kN·m.
2. **Conditions aux limites :**
   - Extrémité libre sans couple : $M = 0$, $T = 0$.
   - Appui simple / articulé d'extrémité : $M = 0$.
3. **Discontinuité de l'effort tranchant :** $\Delta T(x_i) = F_{y,i}$.
4. **Continuité du moment fléchissant :** $\Delta M(x_i) = -C_i$ (saut égal au couple ponctuel appliqué).
5. **Dérivée :** $\left.\frac{dM}{dx}\right|_x = T(x)$.

---

## 5. Caractéristiques Géométriques des Sections & Contraintes (Phase 2)

### 5.1 Théorème de Huygens (Moments quadratiques composés)
Pour une section décomposée en sous-domaines $A_i$ de centres de gravité $G_i(y_i)$ :
$$y_G = \frac{\sum A_i \cdot y_i}{\sum A_i}$$
$$I_z = \sum \left( I_{z,0i} + A_i \cdot d_i^2 \right) \quad \text{avec } d_i = |y_i - y_G|$$

- **Section rectangulaire ($b \times h$) :**
  $$A = b \cdot h, \quad I_z = \frac{b h^3}{12}, \quad W_{\text{el}} = \frac{b h^2}{6}, \quad S_{\max} = \frac{b h^2}{8}$$
- **Profilé en I symétrique ($b, h, t_f, t_w$) :**
  $$I_z = \frac{b h^3 - (b - t_w)(h - 2 t_f)^3}{12}$$
- **Section circulaire ($D$) :**
  $$A = \frac{\pi D^2}{4}, \quad I_z = \frac{\pi D^4}{64}, \quad W_{\text{el}} = \frac{\pi D^3}{32}, \quad \tau_{\max} = \frac{4 T}{3 A}$$

### 5.2 Contraintes Normales (Formule de Navier)
$$\sigma(x, y) = \frac{N(x)}{A} + \frac{M(x) \cdot y}{I_z}$$
- Fibres inférieures tendues ($M > 0$) :
  - En fibre inférieure ($y = -y_{\text{inf}}$) : $\sigma_{\text{inf}} = \frac{N}{A} + \frac{M \cdot y_{\text{inf}}}{I_z} > 0$ (Traction).
  - En fibre supérieure ($y = +y_{\text{sup}}$) : $\sigma_{\text{sup}} = \frac{N}{A} - \frac{M \cdot y_{\text{sup}}}{I_z} < 0$ (Compression).

### 5.3 Contraintes Tangentielles de Cisaillement (Jourawski / Colignon)
$$\tau(x, y) = \frac{|T(x)| \cdot S(y)}{I_z \cdot b(y)}$$
où $S(y) = \int_y^{y_{\text{sup}}} \eta \, dA$ est le moment statique de la partie située au-dessus de la coupure.
Sur l'axe neutre ($y = 0$), $S = S_{\max}$ et le cisaillement $\tau_{\max}$ est maximal.

### 5.4 Critère de Rupture de Von Mises (ELU)
$$\sigma_{VM} = \sqrt{\sigma^2 + 3 \tau^2} \le f_y$$
$$\eta = \frac{\sigma_{VM}}{f_y} \times 100\% \le 100\%$$

---

## 6. Ligne Élastique, Flèches et Rotations (Phase 3)

### 6.1 Équation Différentielle Fondamentale
$$E I_z \cdot \frac{d^2 v(x)}{dx^2} = M(x)$$
- Rotation de section :
  $$\theta(x) = \frac{dv(x)}{dx} = \int \frac{M(x)}{E I_z} dx + C_1$$
- Flèche verticale $f(x) = -v(x)$ (en mm) :
  $$v(x) = \iint \frac{M(x)}{E I_z} dx^2 + C_1 x + C_2$$

### 6.2 Formules Analytiques des Cas Types
| Configuration | Rigidité $EI$ | Flèche maximale $f_{\max}$ | Position de $f_{\max}$ | Rotation aux appuis $\theta$ |
|---|---|---|---|---|
| **Bi-appuyée + Charge uniforme $q$** | $E I_z$ | $$f_{\max} = \frac{5 q L^4}{384 E I_z}$$ | $x = \frac{L}{2}$ | $$\theta = \mp \frac{q L^3}{24 E I_z}$$ |
| **Bi-appuyée + Force ponctuelle $F$ au centre** | $E I_z$ | $$f_{\max} = \frac{F L^3}{48 E I_z}$$ | $x = \frac{L}{2}$ | $$\theta = \mp \frac{F L^2}{16 E I_z}$$ |
| **Console encastrée en 0 + Force $F$ à l'extrémité** | $E I_z$ | $$f_{\max} = \frac{F L^3}{3 E I_z}$$ | $x = L$ | $$\theta(L) = \frac{F L^2}{2 E I_z}$$ |
| **Console encastrée en 0 + Charge uniforme $q$** | $E I_z$ | $$f_{\max} = \frac{q L^4}{8 E I_z}$$ | $x = L$ | $$\theta(L) = \frac{q L^3}{6 E I_z}$$ |

### 6.3 Vérification à l'État Limite de Service (ELS)
$$f_{\max} \le f_{\text{lim}} = \frac{L}{500}$$
$$\eta_{\text{ELS}} = \frac{f_{\max}}{f_{\text{lim}}} \times 100\% \le 100\%$$


