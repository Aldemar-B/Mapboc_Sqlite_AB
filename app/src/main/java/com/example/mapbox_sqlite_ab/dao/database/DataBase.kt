package com.example.mapbox_sqlite_ab.dao.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.example.mapbox_sqlite_ab.BuildConfig
import java.io.*


class DataBase(context: Context, DBNAME: String?) :
    SQLiteOpenHelper(context, DBNAME, null, DB_VERSION) {
    private var myContext: Context? = null
    private var DB_NAME: String? = null

    /**
     * Crea una base de datos vacia en el sistema y la reescribe con nuestro
     * fichero de base de datos.
     */
    fun createDataBase() {

        val dbExist = checkDataBase(DB_NAME)
        if (!dbExist) {
            this.readableDatabase
            try {

                copyDataBase()
            } catch (e: Exception) {
                e.printStackTrace()
                throw Error("Error copiando Base de Datos")
            }
        }
    }

    /**
     * Comprueba si la base de datos existe para evitar copiar siempre el
     * fichero cada vez que se abra la aplicacion.
     *
     * @return true si existe, false si no existe
     */
    private fun checkDataBase(nameDB: String?): Boolean {
        var checkDB: SQLiteDatabase? = null
        try {
            val myPath = DB_PATH + File.separator + nameDB
            val db = File(myPath)
            if (db.exists()) {
                checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE)
                val currentVersion = BuildConfig.VERSION_CODE
                val versionCode = DaoVersionCode(checkDB)
                if (versionCode.loadVersionCode() != -1 && versionCode.loadVersionCode() != currentVersion) {
                    SQLiteDatabase.deleteDatabase(db)
                    checkDB = null
                }
            }
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
        checkDB?.close()
        return checkDB != null
    }

    /**
     * Copia nuestra base de datos desde la carpeta assets a la recien creada
     * base de datos en la carpeta de sistema, desde donde podremos acceder a
     * ella. Esto se hace con bytestream.
     */
    private fun copyDataBase() {
        var myOutput: OutputStream? = null
        var myInput: InputStream? = null
        try {
            // Abrimos el fichero de base de datos como entrada
            myInput = myContext!!.assets.open(DB_NAME!!)

            // Ruta a la base de datos vacia recien creada
            val outFileName = DB_PATH + File.separator + DB_NAME

            // Abrimos la base de datos vacia como salida
            myOutput = FileOutputStream(outFileName)

            // Transferimos los bytes desde el fichero de entrada al de salida
            val buffer = ByteArray(1024)
            var length: Int
            while (myInput.read(buffer).also { length = it } > 0) {
                myOutput.write(buffer, 0, length)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            try {
                myOutput!!.flush()
                myOutput.close()
                myInput?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }// Abre la base de datos

    /**
     * Inicia el proceso de copia del fichero de base de datos, o crea una base
     * de datos vacia en su lugar
     */
    val dataBase: SQLiteDatabase?
        get() {
            var db: SQLiteDatabase? = null
            try {
                // Abre la base de datos
                try {
                    createDataBase()
                } catch (e: Exception) {
                    throw Error("Ha sido imposible crear la Base de Datos:$DB_NAME")
                }
                val myPath = DB_PATH + File.separator + DB_NAME
                db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE)
                val versionCode = DaoVersionCode(db)
                versionCode.saveVersionCode(BuildConfig.VERSION_CODE)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return db
        }

    override fun onCreate(db: SQLiteDatabase) {}
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (newVersion > oldVersion) {
                this.readableDatabase
                copyDataBase()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Error("Error actualizando Base de Datos")
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.disableWriteAheadLogging()
    }

    companion object {
        private var DB_PATH: String? = null
        private const val DB_VERSION = 1
    }

    /**
     * Constructor: Toma referencia hacia el contexto de la aplicacion que lo
     * invoca para poder acceder a los 'assets' y 'resources' de la aplicacion.
     * Crea un objeto DBOpenHelper que nos permitira controlar la apertura de la
     * base de datos.
     *
     * @param context
     */
    init {
        try {
            DB_NAME = DBNAME
            DB_PATH = context.getDatabasePath(DB_NAME).parent
            myContext = context
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
