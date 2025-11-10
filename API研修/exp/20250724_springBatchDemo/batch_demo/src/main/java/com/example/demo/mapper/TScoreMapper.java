package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.TScore;

@Mapper
public interface TScoreMapper {
    public TScore selectAll();
}
