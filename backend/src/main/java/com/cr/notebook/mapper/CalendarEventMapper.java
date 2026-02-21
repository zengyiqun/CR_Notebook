package com.cr.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cr.notebook.entity.CalendarEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CalendarEventMapper extends BaseMapper<CalendarEvent> {
}
