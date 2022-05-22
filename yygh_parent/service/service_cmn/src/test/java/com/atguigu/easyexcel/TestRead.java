package com.atguigu.easyexcel;

import com.alibaba.excel.EasyExcel;

public class TestRead {
    public static void main(String[] args) {
        //设置excel文件路径和文件名称
        String fileName="D:\\excel\\01.xlsx";
        //调用方法实现读取
        EasyExcel.read(fileName,UserData.class,new ExcelListener()).sheet().doRead();
    }
}
