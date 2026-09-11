r"""Paso 3 del README de docs/requisitos: puntos de corte dentro de \texttt{}.

pdfTeX no puede partir una ruta como
`Backend/src/main/java/com/biopet/integration/ExternalApiClient.java` porque
no hay ningun punto de division valido: `/` y `.` no son puntos de corte y la
fuente teletype no tiene guionado suficiente. El resultado es texto fuera del
margen derecho.

Este paso inserta `\allowbreak{}` (una penalizacion de corte de coste cero,
que NO imprime guion) despues de los separadores naturales de rutas e
identificadores: `/`, `.` seguido de caracter alfanumerico, `\_` y `-`. Asi
la linea se puede partir donde un lector espera, sin introducir ningun
caracter que pueda confundirse con parte del nombre.

Solo se modifica el contenido de los argumentos de \texttt{}; el resto del
documento queda intacto.

Uso: python3 paso3_cortes_texttt.py <ruta-al-.tex>
"""
import pathlib
import re
import sys

tex = pathlib.Path(sys.argv[1])
t = tex.read_text(encoding="utf-8")

AB = r"\allowbreak{}"


def fin_argumento(s, inicio):
    """Devuelve el indice del '}' que cierra el argumento abierto en `inicio`."""
    nivel = 0
    i = inicio
    while i < len(s):
        c = s[i]
        if c == "\\":
            i += 2
            continue
        if c == "{":
            nivel += 1
        elif c == "}":
            nivel -= 1
            if nivel == 0:
                return i
        i += 1
    return -1


def con_cortes(contenido):
    if AB in contenido:
        return contenido
    salida = []
    i = 0
    desde_ultimo_corte = 0
    MAX_SEGMENTO = 16  # p. ej. un SHA de 40 caracteres no tiene separadores
    while i < len(contenido):
        c = contenido[i]
        if c == "\\" and contenido[i:i + 2] == r"\_":
            salida.append(r"\_")
            i += 2
            if i < len(contenido):
                salida.append(AB)
            continue
        if c == "\\":
            # secuencia de control: copiar el comando completo sin tocarlo
            m = re.match(r"\\[A-Za-z]+|\\.", contenido[i:])
            salida.append(m.group(0))
            i += len(m.group(0))
            continue
        salida.append(c)
        i += 1
        desde_ultimo_corte += 1
        if i < len(contenido):
            siguiente = contenido[i]
            corta = False
            if c in "/,;":
                corta = True
            elif c == "." and (siguiente.isalnum() or siguiente == "\\"):
                corta = True
            elif c == "-" and siguiente.isalnum():
                corta = True
            elif (c.islower() or c.isdigit()) and siguiente.isupper():
                # frontera camelCase: punto de corte natural dentro de un
                # identificador largo (adminActualiza|Mascota|Asignandola...)
                corta = True
            elif desde_ultimo_corte >= MAX_SEGMENTO and siguiente.isalnum():
                # ultimo recurso: cadenas sin ningun separador, como un SHA
                corta = True
            if corta:
                salida.append(AB)
                desde_ultimo_corte = 0
    return "".join(salida)


resultado = []
i = 0
cambiados = 0
while True:
    j = t.find(r"\texttt{", i)
    if j == -1:
        resultado.append(t[i:])
        break
    abre = j + len(r"\texttt")
    cierra = fin_argumento(t, abre)
    if cierra == -1:
        resultado.append(t[i:])
        break
    resultado.append(t[i:abre + 1])
    original = t[abre + 1:cierra]
    nuevo = con_cortes(original)
    if nuevo != original:
        cambiados += 1
    resultado.append(nuevo)
    resultado.append("}")
    i = cierra + 1

tex.write_text("".join(resultado), encoding="utf-8")
print("Puntos de corte insertados en %d argumentos de \\texttt{}." % cambiados)
