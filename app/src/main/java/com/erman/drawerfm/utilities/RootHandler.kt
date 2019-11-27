package com.erman.drawerfm.utilities

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

fun getRootAccess()
{
    val p: Process
    try { // Preform su to get root privledges
        p = Runtime.getRuntime().exec("su")
        // Attempt to write a file to a root-only
        //val os = DataOutputStream(p.outputStream)
        //os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n")
        // Close the terminal
       // os.writeBytes("exit\n")
        //os.flush()
        try {
            p.waitFor()
            if (p.exitValue() != 255) { // TODO Code to run on success
                System.out.println("aaaaaaaaaaaaaaaaaaaaa")
            } else { // TODO Code to run on unsuccessful
                Log.e("not root","")
            }
        } catch (e: InterruptedException) { // TODO Code to run in interrupted exception
            Log.e("not root","")
        }
    } catch (e: IOException) { // TODO Code to run in input/output exception
        Log.e("not root","")
    }

}