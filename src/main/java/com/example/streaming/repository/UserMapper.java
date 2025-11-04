package com.example.streaming.repository;

import com.example.streaming.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.ResultHandler;

import java.util.List;

@Mapper
public interface UserMapper {

    List<UserDto> findAllUsers();

    // 스트리밍용 메소드 - void 리턴, ResultHandler 파라미터
    void findAllUsersStreaming(ResultHandler<UserDto> handler);

}