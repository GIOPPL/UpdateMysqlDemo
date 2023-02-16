package com.potato.demo3;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

public class KeyBoardHelper {
    public static void main(String[] args) {
        new MyFrame("KeyEvent test").LaunchMyFrame();
    }

    static class MyFrame extends Frame {
        MyFrame(String s) {                            //构造方法
            super(s);
        }

        public void LaunchMyFrame() {                 //定义一个运行窗体方法
            setLayout(null);                            //设置默认布局
            setSize(300, 333);                           //设置窗体大小
            this.setBackground(new Color(211, 222, 233)); //设置窗体背景色
            setVisible(true);                           //设置窗体可见
            this.addKeyListener(new Monitor());         //Monitor要实现KeyListener接口(键盘监听）
        }

        class Monitor extends KeyAdapter {            //内部类，实现KeyListener的子类KeyAdapter
            public void keyPressed(KeyEvent e) {       //重写要实现的按下按键的方法
                int key = e.getKeyCode();                 //获取按下按键的虚拟码(int类型）
                System.out.println(key);
                switch (key) {
                    case KeyEvent.VK_1:
                        copy("@ColumnDescribe(isNotNull = false)");
                        break;
                    case KeyEvent.VK_2:
                        copy("@ColumnDescribe(length = 64)");
                        break;
                    case KeyEvent.VK_3:
                        copy("");
                        break;
                }
            }
            void copy(String s){
                StringSelection stsel = new StringSelection(s);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
            }
        }
    }
}
