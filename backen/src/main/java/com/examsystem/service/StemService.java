package com.examsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.examsystem.entity.Stem;
import com.examsystem.mapper.StemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StemService {

    @Autowired
    private StemMapper stemMapper;

    public Stem getStemById(Long id) {
        return stemMapper.selectById(id);
    }

    public Map<Long, Stem> getStemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            // 如果没有提供ids，返回所有stem
            List<Stem> stems = stemMapper.selectList(null);
            return stems.stream().collect(Collectors.toMap(Stem::getId, stem -> stem));
        }
        List<Stem> stems = stemMapper.selectList(new LambdaQueryWrapper<Stem>()
                .in(Stem::getId, ids));
        return stems.stream().collect(Collectors.toMap(Stem::getId, stem -> stem));
    }

    public void createStem(Stem stem) {
        stem.setCreatedTime(LocalDateTime.now());
        stem.setUpdatedTime(LocalDateTime.now());
        stem.setCreateBy(1L); // 默认创建者
        stem.setUpdateBy(1L); // 默认更新者
        stemMapper.insert(stem);
    }

    public void updateStem(Stem stem) {
        stem.setUpdatedTime(LocalDateTime.now());
        stem.setUpdateBy(1L); // 默认更新者
        stemMapper.updateById(stem);
    }
}

