package com.examsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.entity.SysLog;

public interface SysLogService {
    void saveLog(SysLog log);
    IPage<SysLog> getLogs(int page, int size, String keyword);
}