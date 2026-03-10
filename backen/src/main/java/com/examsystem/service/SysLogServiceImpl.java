package com.examsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.entity.SysLog;
import com.examsystem.mapper.SysLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class SysLogServiceImpl implements SysLogService {

    @Resource
    private SysLogMapper sysLogMapper;

    @Override
    public void saveLog(SysLog log) {
        log.setCreateTime(LocalDateTime.now());
        sysLogMapper.insert(log);
    }

    @Override
    public IPage<SysLog> getLogs(int page, int size, String keyword) {
        Page<SysLog> p = new Page<>(page, size);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .orderByDesc(SysLog::getCreateTime);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysLog::getUsername, keyword)
                    .or()
                    .like(SysLog::getOperation, keyword);
        }
        return sysLogMapper.selectPage(p, wrapper);
    }
}
