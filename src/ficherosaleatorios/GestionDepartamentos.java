package ficherosaleatorios;

import org.iesalandalus.programacion.utilidades.Entrada;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

// Aplicación que gestiona departamentos mediante un fichero binario de acceso aleatorio
public class GestionDepartamentos {

    // ===================== CONSTANTES =====================

    static final int LONGITUD_REGISTRO = 64; // 2 + 30 + 30 + 2 bytes
    static final int LONGITUD_TEXTO = 15; // caracteres del nombre y de la localidad
    static final int MAXIMO_DEPARTAMENTO = 32767; // valor máximo de un short

    // Byte en el que empieza cada campo dentro del registro
    static final int DESPLAZAMIENTO_NOMBRE = 2;
    static final int DESPLAZAMIENTO_LOCALIDAD = 32;
    static final int DESPLAZAMIENTO_EMPLEADOS = 62;

    // Marcas de las posiciones nunca utilizadas y de los registros eliminados
    static final String TEXTO_NUNCA_UTILIZADO = "---------------";
    static final String TEXTO_ELIMINADO = "***************";

    // Estados posibles de una posición del fichero
    static final String ESTADO_FUERA = "fuera del fichero";
    static final String ESTADO_NUNCA_UTILIZADA = "nunca utilizada";
    static final String ESTADO_ACTIVA = "activa";
    static final String ESTADO_ELIMINADA = "eliminada";
    static final String ESTADO_ANOMALA = "anómala";

    static File fichero = new File("datos/AleatorioDepart.dat");

    // ===================== MÉTODOS AUXILIARES DE TEXTO =====================

    // Devuelve true si el texto, sin espacios exteriores, no está vacío
    // y no supera los 15 caracteres
    private static boolean esTextoValido(String texto)
    {
        String limpio = texto.trim(); // quitamos los espacios iniciales y finales
        if (!limpio.isEmpty() && limpio.length() <= LONGITUD_TEXTO)
            return true;
        else
            return false;
    }

    // Completa con espacios a la derecha un texto ya validado hasta los 15 caracteres
    private static String rellenarTexto(String texto)
    {
        StringBuffer buffer = new StringBuffer(texto.trim());
        while (buffer.length() < LONGITUD_TEXTO)
            buffer.append(' '); // añadimos un espacio al final
        return buffer.toString();
    }

    // Devuelve true si un texto leído del fichero no está vacío
    // y está completado únicamente con espacios a la derecha
    private static boolean esTextoAlmacenadoValido(String texto)
    {
        return esTextoValido(texto) && texto.equals(rellenarTexto(texto));
    }

    // Lee un texto de 15 caracteres desde la posición actual del puntero.
    // Devuelve el texto con su relleno, sin quitar los espacios
    private static String leerTexto(RandomAccessFile file) throws IOException
    {
        // recorro uno a uno los caracteres del texto
        char texto[] = new char[LONGITUD_TEXTO], aux;
        for (int i = 0; i < texto.length; i++) {
            aux = file.readChar();
            texto[i] = aux; // los voy guardando en el array
        }
        return new String(texto); // convierto a String el array
    }

    // ===================== MÉTODOS AUXILIARES DEL FICHERO =====================

    // Crea la carpeta de datos y el fichero vacío si no existen.
    // Si el fichero ya existe, no se modifica su contenido
    private static boolean prepararFichero()
    {
        try
        {
            File carpeta = new File("datos");
            if (!carpeta.exists())
                carpeta.mkdir(); // creamos la carpeta si no existe

            // createNewFile solo crea el fichero si no existe; nunca borra datos
            if (fichero.createNewFile())
                System.out.println("Se ha creado el fichero vacío " + fichero.getName() + ".");
            else
                System.out.println("Se ha abierto el fichero existente " + fichero.getName() + ".");

            return true;
        }
        catch (IOException e)
        {
            System.out.println("No se ha podido crear el fichero " + fichero.getName() + ".");
            return false;
        }
    }

    // Devuelve el estado de una posición (empezando en 1) leyendo su registro completo:
    // fuera del fichero, nunca utilizada, activa, eliminada o anómala
    private static String estadoPosicion(RandomAccessFile file, int numeroPosicion) throws IOException
    {
        long posicion = (numeroPosicion - 1) * LONGITUD_REGISTRO;

        if (posicion >= file.length())
            return ESTADO_FUERA; // la posición está en el final del fichero o más allá

        if (posicion + LONGITUD_REGISTRO > file.length())
            return ESTADO_ANOMALA; // el registro está incompleto

        file.seek(posicion); // nos posicionamos
        short numeroDepartamento = file.readShort(); // obtengo el número de departamento
        String nombre = leerTexto(file); // obtengo el nombre
        String localidad = leerTexto(file); // obtengo la localidad
        short numeroEmpleados = file.readShort(); // obtengo el número de empleados

        if (numeroDepartamento == 0 && nombre.equals(TEXTO_NUNCA_UTILIZADO)
                && localidad.equals(TEXTO_NUNCA_UTILIZADO) && numeroEmpleados == 0)
            return ESTADO_NUNCA_UTILIZADA;

        if (numeroDepartamento == -1 && nombre.equals(TEXTO_ELIMINADO)
                && localidad.equals(TEXTO_ELIMINADO) && numeroEmpleados == -1)
            return ESTADO_ELIMINADA;

        if (numeroDepartamento == numeroPosicion && numeroEmpleados >= 0
                && esTextoAlmacenadoValido(nombre) && esTextoAlmacenadoValido(localidad))
            return ESTADO_ACTIVA;

        return ESTADO_ANOMALA; // no cumple ninguno de los estados anteriores
    }

    // Escribe un registro completo a partir de la posición actual del puntero.
    // Los textos deben tener ya exactamente 15 caracteres
    private static void escribirRegistro(RandomAccessFile file, int numero, String nombre,
                                         String localidad, int empleados) throws IOException
    {
        file.writeShort(numero); // insertar número de departamento (2 bytes)
        file.writeChars(nombre); // insertar nombre (30 bytes)
        file.writeChars(localidad); // insertar localidad (30 bytes)
        file.writeShort(empleados); // insertar número de empleados (2 bytes)
    }

    // ===================== MÉTODOS PRINCIPALES =====================

    // Comprueba si existe el departamento indicado, es decir, si su posición
    // contiene un registro activo. Informa del estado concreto de la posición
    private static boolean existeDepartamento(int numeroDepartamento)
    {
        // Validación del identificador: no debe confundirse con un departamento inexistente
        if (numeroDepartamento < 1 || numeroDepartamento > MAXIMO_DEPARTAMENTO) {
            System.out.println("ERROR: el número de departamento debe estar entre 1 y " + MAXIMO_DEPARTAMENTO + ".");
            return false;
        }

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "r");
            String estado = estadoPosicion(file, numeroDepartamento);
            file.close(); // cerrar fichero

            if (estado.equals(ESTADO_ACTIVA))
                return true;

            if (estado.equals(ESTADO_FUERA))
                System.out.printf("El departamento %d no existe: su posición está fuera del fichero.%n", numeroDepartamento);
            else if (estado.equals(ESTADO_NUNCA_UTILIZADA))
                System.out.printf("El departamento %d no existe: su posición nunca se ha utilizado.%n", numeroDepartamento);
            else if (estado.equals(ESTADO_ELIMINADA))
                System.out.printf("El departamento %d no existe: su registro está eliminado.%n", numeroDepartamento);
            else
                System.out.printf("El departamento %d no existe: su posición contiene un registro anómalo.%n", numeroDepartamento);

            return false;
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
        return false;
    }

    // Muestra todos los datos del departamento indicado accediendo directamente a su posición
    private static void consultaDepartamento(int numeroDepartamento)
    {
        if (!existeDepartamento(numeroDepartamento))
            return; // existeDepartamento ya ha mostrado el motivo

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "r");

            long posicion = (numeroDepartamento - 1) * LONGITUD_REGISTRO;
            file.seek(posicion); // nos posicionamos

            short numero = file.readShort(); // obtengo el número de departamento
            String nombre = leerTexto(file).trim(); // obtengo el nombre sin relleno
            String localidad = leerTexto(file).trim(); // obtengo la localidad sin relleno
            short empleados = file.readShort(); // obtengo el número de empleados

            System.out.printf("Número: %d, Nombre: %s, Localidad: %s, Empleados: %d%n",
                    numero, nombre, localidad, empleados);

            file.close(); // cerrar fichero
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Inserta un departamento si no existe un registro activo con ese número.
    // Si su posición está más allá del final, inicializa antes los huecos intermedios
    private static void insertaDepartamento(int numeroDepartamento, String nombre,
                                            String localidad, int numeroEmpleados)
    {
        // Validamos todos los datos antes de abrir el fichero
        if (numeroDepartamento < 1 || numeroDepartamento > MAXIMO_DEPARTAMENTO) {
            System.out.println("ERROR: el número de departamento debe estar entre 1 y " + MAXIMO_DEPARTAMENTO + ".");
            return;
        }
        if (!esTextoValido(nombre) || !esTextoValido(localidad)) {
            System.out.println("ERROR: el nombre y la localidad no pueden estar vacíos ni superar " + LONGITUD_TEXTO + " caracteres.");
            return;
        }
        if (numeroEmpleados < 0 || numeroEmpleados > MAXIMO_DEPARTAMENTO) {
            System.out.println("ERROR: el número de empleados debe estar entre 0 y " + MAXIMO_DEPARTAMENTO + ".");
            return;
        }

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "rw");
            String estado = estadoPosicion(file, numeroDepartamento);

            if (estado.equals(ESTADO_ACTIVA)) {
                System.out.printf("El departamento %d ya existe. No se ha modificado el fichero.%n", numeroDepartamento);
            }
            else if (estado.equals(ESTADO_ANOMALA)) {
                System.out.printf("La posición del departamento %d contiene un registro anómalo. No se puede insertar.%n", numeroDepartamento);
            }
            else {
                long posicion = (numeroDepartamento - 1) * LONGITUD_REGISTRO;

                // Si la posición está fuera del fichero, rellenamos los huecos intermedios
                if (estado.equals(ESTADO_FUERA)) {
                    file.seek(file.length()); // nos posicionamos al final del fichero
                    while (file.getFilePointer() < posicion)
                        escribirRegistro(file, 0, TEXTO_NUNCA_UTILIZADO, TEXTO_NUNCA_UTILIZADO, 0);
                }

                file.seek(posicion); // nos posicionamos en el registro del departamento
                escribirRegistro(file, numeroDepartamento, rellenarTexto(nombre),
                        rellenarTexto(localidad), numeroEmpleados);
                System.out.printf("Departamento %d insertado correctamente.%n", numeroDepartamento);
            }

            file.close(); // cerrar fichero
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Actualiza únicamente el número de empleados de un departamento existente,
    // situando el puntero directamente sobre ese campo
    private static void actualizaNumeroEmpleados(int numeroDepartamento, int nuevoNumeroEmpleados)
    {
        if (nuevoNumeroEmpleados < 0 || nuevoNumeroEmpleados > MAXIMO_DEPARTAMENTO) {
            System.out.println("ERROR: el número de empleados debe estar entre 0 y " + MAXIMO_DEPARTAMENTO + ".");
            return;
        }
        if (!existeDepartamento(numeroDepartamento))
            return; // existeDepartamento ya ha mostrado el motivo

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "rw");

            long posicion = (numeroDepartamento - 1) * LONGITUD_REGISTRO;
            file.seek(posicion + DESPLAZAMIENTO_EMPLEADOS);
            file.writeShort(nuevoNumeroEmpleados);// modificar número de empleados

            file.close(); // cerrar fichero
            System.out.printf("Número de empleados del departamento %d actualizado.%n", numeroDepartamento);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Actualiza el nombre y la localidad de un departamento existente.
    // Valida los dos textos antes de escribir; si alguno es inválido no modifica ninguno
    private static void actualizaNombreLocalidad(int numeroDepartamento, String nuevoNombre, String nuevaLocalidad)
    {
        if (!esTextoValido(nuevoNombre) || !esTextoValido(nuevaLocalidad)) {
            System.out.println("ERROR: el nombre y la localidad no pueden estar vacíos ni superar " + LONGITUD_TEXTO + " caracteres. No se ha modificado nada.");
            return;
        }
        if (!existeDepartamento(numeroDepartamento))
            return; // existeDepartamento ya ha mostrado el motivo

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "rw");

            long posicion = (numeroDepartamento - 1) * LONGITUD_REGISTRO;
            file.seek(posicion + DESPLAZAMIENTO_NOMBRE); // nos posicionamos en el campo nombre
            file.writeChars(rellenarTexto(nuevoNombre)); // modificar nombre (30 bytes)
            file.seek(posicion + DESPLAZAMIENTO_LOCALIDAD); // nos posicionamos en el campo localidad
            file.writeChars(rellenarTexto(nuevaLocalidad)); // modificar localidad (30 bytes)

            file.close(); // cerrar fichero
            System.out.printf("Nombre y localidad del departamento %d actualizados.%n", numeroDepartamento);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Realiza el borrado lógico de un departamento existente escribiendo las marcas
    // de borrado en sus cuatro campos. No reduce la longitud del fichero
    private static void eliminaDepartamento(int numeroDepartamento)
    {
        if (!existeDepartamento(numeroDepartamento))
            return; // existeDepartamento ya ha mostrado el motivo

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "rw");

            long posicion = (numeroDepartamento - 1) * LONGITUD_REGISTRO;
            file.seek(posicion); // nos posicionamos al inicio del registro
            escribirRegistro(file, -1, TEXTO_ELIMINADO, TEXTO_ELIMINADO, -1);// marcamos el registro como eliminado

            file.close(); // cerrar fichero
            System.out.printf("Departamento %d eliminado.%n", numeroDepartamento);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Muestra todos los departamentos activos ordenados por número, recorriendo
    // secuencialmente el fichero. Al final indica cuántos se han mostrado
    private static void listadoDepartamentos()
    {
        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "r");

            long totalPosiciones = file.length() / LONGITUD_REGISTRO; // posiciones completas
            int mostrados = 0; // contador de departamentos activos mostrados

            for (int numeroPosicion = 1; numeroPosicion <= totalPosiciones; numeroPosicion++) {
                if (estadoPosicion(file, numeroPosicion).equals(ESTADO_ACTIVA)) {
                    file.seek((numeroPosicion - 1) * LONGITUD_REGISTRO); // volvemos al inicio del registro
                    short numero = file.readShort(); // obtengo el número de departamento
                    String nombre = leerTexto(file).trim(); // obtengo el nombre sin relleno
                    String localidad = leerTexto(file).trim(); // obtengo la localidad sin relleno
                    short empleados = file.readShort(); // obtengo el número de empleados

                    System.out.printf("Número: %d, Nombre: %s, Localidad: %s, Empleados: %d%n",
                            numero, nombre, localidad, empleados);
                    mostrados++;
                }
            }

            file.close(); // cerrar fichero

            if (mostrados == 0)
                System.out.println("No hay ningún departamento activo.");
            else
                System.out.println("Departamentos mostrados: " + mostrados);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Muestra todas las posiciones completas del fichero, incluidas las nunca utilizadas
    // y las eliminadas, con su desplazamiento, identificadores y estado
    private static void inspeccionFisica()
    {
        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "r");

            long totalPosiciones = file.length() / LONGITUD_REGISTRO; // posiciones completas
            if (totalPosiciones == 0)
                System.out.println("El fichero no contiene ninguna posición completa.");
            else
                System.out.printf("%-10s %-16s %-10s %-12s %s%n",
                        "Posición", "Desplazamiento", "Esperado", "Almacenado", "Estado");

            for (int numeroPosicion = 1; numeroPosicion <= totalPosiciones; numeroPosicion++) {
                long desplazamiento = (numeroPosicion - 1) * LONGITUD_REGISTRO; // byte en el que empieza la posición
                String estado = estadoPosicion(file, numeroPosicion);

                file.seek(desplazamiento); // volvemos al inicio del registro
                short almacenado = file.readShort(); // obtengo el identificador almacenado

                System.out.printf("%-10d %-16d %-10d %-12d %s%n",
                        numeroPosicion, desplazamiento, numeroPosicion, almacenado, estado);
            }

            file.close(); // cerrar fichero
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Muestra la longitud del fichero, el número de posiciones de cada estado,
    // la suma de empleados de los departamentos activos y el porcentaje de ocupación
    private static void estadisticas()
    {
        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "r");

            long totalPosiciones = file.length() / LONGITUD_REGISTRO; // posiciones completas
            int activas = 0, eliminadas = 0, nuncaUtilizadas = 0; // contadores de cada estado
            int sumaEmpleados = 0;

            for (int numeroPosicion = 1; numeroPosicion <= totalPosiciones; numeroPosicion++) {
                String estado = estadoPosicion(file, numeroPosicion);

                if (estado.equals(ESTADO_ACTIVA)) {
                    activas++;
                    long posicion = (numeroPosicion - 1) * LONGITUD_REGISTRO;
                    file.seek(posicion + DESPLAZAMIENTO_EMPLEADOS); // nos posicionamos en los empleados
                    sumaEmpleados = sumaEmpleados + file.readShort(); // acumulamos los empleados
                } else if (estado.equals(ESTADO_ELIMINADA))
                    eliminadas++;
                else if (estado.equals(ESTADO_NUNCA_UTILIZADA)) {
                    nuncaUtilizadas++;
                }
            }

            System.out.println("Longitud del fichero: " + file.length() + " bytes");
            System.out.println("Departamentos activos: " + activas);
            System.out.println("Posiciones eliminadas: " + eliminadas);
            System.out.println("Posiciones nunca utilizadas: " + nuncaUtilizadas);
            System.out.println("Suma de empleados de los departamentos activos: " + sumaEmpleados);

            // Porcentaje de ocupación: activas respecto a eliminadas más nunca utilizadas
            if (eliminadas + nuncaUtilizadas == 0)
                System.out.println("Porcentaje de ocupación: no hay posiciones eliminadas ni nunca utilizadas.");
            else
                System.out.printf("Porcentaje de ocupación: %.2f %%%n",
                        (double) activas / (eliminadas + nuncaUtilizadas) * 100);

            file.close(); // cerrar fichero
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
    }

    // Comprueba la integridad del fichero e informa de cada problema y su posición en bytes.
    // Es una operación de diagnóstico: no repara ni sobrescribe los datos.
    // Devuelve true si no se detecta ningún problema
    private static boolean integridad()
    {
        int problemas = 0; // contador de problemas detectados

        try
        {
            RandomAccessFile file = new RandomAccessFile(fichero, "r");

            // Regla 1: la longitud debe ser múltiplo de 64
            if (file.length() % LONGITUD_REGISTRO != 0) {
                long inicioIncompleto = (file.length() / LONGITUD_REGISTRO) * LONGITUD_REGISTRO;
                System.out.printf("Byte %d: la longitud del fichero (%d bytes) no es múltiplo de %d.%n",
                        inicioIncompleto, file.length(), LONGITUD_REGISTRO);
                problemas++;
            }

            long totalPosiciones = file.length() / LONGITUD_REGISTRO; // posiciones completas

            for (int numeroPosicion = 1; numeroPosicion <= totalPosiciones; numeroPosicion++) {
                long posicion = (numeroPosicion - 1) * LONGITUD_REGISTRO;
                file.seek(posicion); // nos posicionamos
                short identificador = file.readShort(); // obtengo el identificador
                String nombre = leerTexto(file); // obtengo el nombre con su relleno
                String localidad = leerTexto(file); // obtengo la localidad con su relleno
                short empleados = file.readShort(); // obtengo el número de empleados

                if (identificador == 0) {
                    // Regla 6: posición nunca utilizada con formato incorrecto
                    if (!nombre.equals(TEXTO_NUNCA_UTILIZADO) || !localidad.equals(TEXTO_NUNCA_UTILIZADO)
                            || empleados != 0) {
                        System.out.printf("Byte %d: posición nunca utilizada que no respeta su formato.%n", posicion);
                        problemas++;
                    }
                }
                else if (identificador == -1) {
                    // Regla 7: registro eliminado sin las marcas de borrado completas
                    if (!nombre.equals(TEXTO_ELIMINADO) || !localidad.equals(TEXTO_ELIMINADO)
                            || empleados != -1) {
                        System.out.printf("Byte %d: registro eliminado sin las marcas de borrado completas.%n", posicion);
                        problemas++;
                    }
                }
                else if (identificador < -1) {
                    // Regla 2: identificador negativo distinto de -1
                    System.out.printf("Byte %d: identificador negativo no válido (%d).%n", posicion, identificador);
                    problemas++;
                }
                else {
                    // El identificador es positivo: debe ser un registro activo válido
                    if (identificador != numeroPosicion) {
                        // Regla 3
                        System.out.printf("Byte %d: el identificador %d no corresponde a la posición %d.%n",
                                posicion, identificador, numeroPosicion);
                        problemas++;
                    }
                    if (empleados < 0) {
                        // Regla 4
                        System.out.printf("Byte %d: número de empleados negativo (%d) en un registro activo.%n",
                                posicion, empleados);
                        problemas++;
                    }
                    if (!esTextoAlmacenadoValido(nombre)) {
                        // Regla 5
                        System.out.printf("Byte %d: nombre vacío o no válido en un registro activo.%n", posicion);
                        problemas++;
                    }
                    if (!esTextoAlmacenadoValido(localidad)) {
                        // Regla 5
                        System.out.printf("Byte %d: localidad vacía o no válida en un registro activo.%n", posicion);
                        problemas++;
                    }
                }
            }

            file.close(); // cerrar fichero

            if (problemas == 0)
                System.out.println("Integridad correcta: no se han detectado problemas.");
            else if (problemas == 1)
                System.out.println("Se ha detectado 1 problema de integridad.");
            else
                System.out.println("Se han detectado " + problemas + " problemas de integridad.");

            return problemas == 0;
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR DE ACCESO: el fichero " + fichero.getName() + " no ha sido encontrado.");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("ERROR DE ACCESO: el modo de lectura escritura del fichero no es correcto.");
        }
        catch (IOException e)
        {
            System.out.println("ERROR DE ACCESO: se ha producido un error de E/S en el fichero.");
        }
        return false; // si no se ha podido revisar el fichero, no se considera íntegro
    }

    // ===================== MÉTODOS DE ENTRADA POR TECLADO =====================
    // Pide un número de departamento hasta que esté entre 1 y 32767
    private static int pedirNumeroDepartamento()
    {
        int numero;
        do {
            System.out.print("Introduce el número de departamento (1-" + MAXIMO_DEPARTAMENTO + "): ");
            numero = Entrada.entero();
            if (numero < 1 || numero > MAXIMO_DEPARTAMENTO)
                System.out.println("ERROR: número de departamento no válido.");
        } while (numero < 1 || numero > MAXIMO_DEPARTAMENTO);
        return numero;
    }

    // Pide un número de empleados hasta que esté entre 0 y 32767
    private static int pedirNumeroEmpleados()
    {
        int empleados;
        do {
            System.out.print("Introduce el número de empleados (0-" + MAXIMO_DEPARTAMENTO + "): ");
            empleados = Entrada.entero();
            if (empleados < 0 || empleados > MAXIMO_DEPARTAMENTO)
                System.out.println("ERROR: número de empleados no válido.");
        } while (empleados < 0 || empleados > MAXIMO_DEPARTAMENTO);
        return empleados;
    }

    // Pide un texto hasta que sea válido: no vacío y de 15 caracteres como máximo
    private static String pedirTexto(String mensaje)
    {
        String texto;
        do {
            System.out.print(mensaje);
            texto = Entrada.cadena();
            if (!esTextoValido(texto))
                System.out.println("ERROR: el texto no puede estar vacío ni superar " + LONGITUD_TEXTO + " caracteres.");
        } while (!esTextoValido(texto));
        return texto;
    }

    // Pide confirmación al usuario y devuelve true si responde "s"
    private static boolean confirmar(String pregunta)
    {
        String respuesta;
        do {
            System.out.print(pregunta + " (s/n): ");
            respuesta = Entrada.cadena();
        } while (!respuesta.equalsIgnoreCase("s") && !respuesta.equalsIgnoreCase("n"));
        return respuesta.equalsIgnoreCase("s");
    }

    // Muestra las opciones del menú. En modo diagnóstico solo muestra las permitidas
    private static void mostrarMenu(boolean modoDiagnostico)
    {
        System.out.println();
        System.out.println("===== GESTIÓN DE DEPARTAMENTOS =====");
        if (modoDiagnostico)
            System.out.println("(MODO DIAGNÓSTICO)");
        else {
            System.out.println("1. Insertar departamento");
            System.out.println("2. Consultar departamento");
            System.out.println("3. Modificar número de empleados");
            System.out.println("4. Modificar nombre y localidad");
            System.out.println("5. Eliminar departamento");
            System.out.println("6. Listado de departamentos");
        }
        System.out.println("7. Inspección física del fichero");
        System.out.println("8. Estadísticas");
        System.out.println("9. Comprobar integridad");
        System.out.println("0. Salir");
        System.out.print("Elige una opción: ");
    }

    // ===================== MAIN =====================

    public static void main(String[] args) {
        boolean ficheroExistia = fichero.exists(); // lo comprobamos antes de crearlo
        boolean modoDiagnostico = false;

        if (!prepararFichero()) {
            System.out.println("La aplicación no puede continuar sin el fichero de datos.");
            return;
        }

        // Si el fichero ya existía, comprobamos su integridad al arrancar
        if (ficheroExistia) {
            System.out.println("Comprobando la integridad del fichero...");
            if (!integridad()) {
                modoDiagnostico = true;
                System.out.println("ATENCIÓN: se han detectado anomalías. La aplicación funcionará en modo diagnóstico:");
                System.out.println("solo se permitirán las operaciones de inspección, estadísticas e integridad.");
            }
        }

        int opcion;
        do {
            mostrarMenu(modoDiagnostico);
            opcion = Entrada.entero();

            // En modo diagnóstico solo se permiten las opciones 7, 8, 9 y 0
            if (modoDiagnostico && opcion >= 1 && opcion <= 6) {
                System.out.println("Operación no disponible en modo diagnóstico.");
            }
            else {
                switch (opcion) {
                    case 1:
                        int numeroNuevo = pedirNumeroDepartamento();
                        String nombre = pedirTexto("Introduce el nombre: ");
                        String localidad = pedirTexto("Introduce la localidad: ");
                        int empleados = pedirNumeroEmpleados();
                        insertaDepartamento(numeroNuevo, nombre, localidad, empleados);
                        break;
                    case 2:
                        int numeroConsulta = pedirNumeroDepartamento();
                        consultaDepartamento(numeroConsulta);
                        break;
                    case 3:
                        int numeroEmpleadosDep = pedirNumeroDepartamento();
                        int nuevosEmpleados = pedirNumeroEmpleados();
                        actualizaNumeroEmpleados(numeroEmpleadosDep, nuevosEmpleados);
                        break;
                    case 4:
                        int numeroTextosDep = pedirNumeroDepartamento();
                        String nuevoNombre = pedirTexto("Introduce el nuevo nombre: ");
                        String nuevaLocalidad = pedirTexto("Introduce la nueva localidad: ");
                        actualizaNombreLocalidad(numeroTextosDep, nuevoNombre, nuevaLocalidad);
                        break;
                    case 5:
                        int numeroBorrar = pedirNumeroDepartamento();
                        if (existeDepartamento(numeroBorrar)) {
                            consultaDepartamento(numeroBorrar); // mostramos qué se va a borrar
                            if (confirmar("¿Seguro que desea eliminar el departamento " + numeroBorrar + "?"))
                                eliminaDepartamento(numeroBorrar);
                            else
                                System.out.println("Eliminación cancelada.");
                        }
                        break;
                    case 6:
                        listadoDepartamentos();
                        break;
                    case 7:
                        inspeccionFisica();
                        break;
                    case 8:
                        estadisticas();
                        break;
                    case 9:
                        integridad();
                        break;
                    case 0:
                        System.out.println("Fin de la aplicación.");
                        break;
                    default:
                        System.out.println("Opción no válida.");
                }
            }
        } while (opcion != 0);
    }
}