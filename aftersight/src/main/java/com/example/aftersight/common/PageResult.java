package com.example.aftersight.common;

import java.util.List;

public class PageResult <T>{
    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer pages;

}
