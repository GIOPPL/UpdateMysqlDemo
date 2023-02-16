package com.potato.demo3;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class WinKeyBoard implements Runnable{
    public static void main(String[] args) {
        new Thread(new WinKeyBoard()).start();
    }
    private static WinUser.HHOOK hhk;
    private static WinUser.LowLevelKeyboardProc keyboardHook;
    final static User32 lib = User32.INSTANCE;

    public WinKeyBoard(){
    }
    @Override
    public void run() {
        WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        keyboardHook = new WinUser.LowLevelKeyboardProc () {
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT info) {
                if (wParam.toString().equals("256")){
                    if (info.vkCode==112){
                        //F1
                        copy("@ColumnDescribe(length = 64)");
                    }
                    if (info.vkCode==113){
                        //F2
                        copy("@ColumnDescribe(isNotNull = false)");
                    }
                    if (info.vkCode==114){
                        //F3
                        copy(",isNotNull = false");
                    }
                    if (info.vkCode==115){
                        //F4
                        copy("@ColumnDescribe(type = \"longtext\")");
                    }
                }
                return lib.CallNextHookEx(hhk, nCode, wParam, info.getPointer());
            }
        };
        hhk = lib.SetWindowsHookEx(User32.WH_KEYBOARD_LL, keyboardHook, hMod, 0);
        int result;
        WinUser.MSG msg = new WinUser.MSG ();
        while ((result = lib.GetMessage(msg, null, 0, 0)) != 0) {
            System.out.println(result);
        }
        lib.UnhookWindowsHookEx(hhk);
    }
    void copy(String s){
        StringSelection stsel = new StringSelection(s);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
    }
}
