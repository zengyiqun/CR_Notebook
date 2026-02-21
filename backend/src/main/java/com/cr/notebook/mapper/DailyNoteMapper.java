package com.cr.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cr.notebook.entity.DailyNote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyNoteMapper extends BaseMapper<DailyNote> {
}
