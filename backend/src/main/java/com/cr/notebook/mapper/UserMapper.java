package com.cr.notebook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cr.notebook.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
