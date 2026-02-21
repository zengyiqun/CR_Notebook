package com.cr.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cr.notebook.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
