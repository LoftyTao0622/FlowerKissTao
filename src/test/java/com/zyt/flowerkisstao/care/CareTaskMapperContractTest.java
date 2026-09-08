package com.zyt.flowerkisstao.care;

import com.zyt.flowerkisstao.care.domain.entity.CareTask;
import com.zyt.flowerkisstao.care.infrastructure.mapper.CareTaskMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CareTaskMapperContractTest {

    @Test
    void generatedTaskKeepsStablePlannedDate() throws NoSuchMethodException {
        Method method = CareTaskMapper.class.getMethod("insertIgnore", CareTask.class);
        String sql = String.join(" ", method.getAnnotation(Insert.class).value()).toLowerCase();

        assertThat(sql).contains("planned_date", "#{planneddate}");
    }

    @Test
    void userActionsOnlyUpdateOpenTasks() throws NoSuchMethodException {
        assertOpenStatePredicate("completeIfOpen", CareTask.class);
        assertOpenStatePredicate("skipIfOpen", Long.class);
        assertOpenStatePredicate("postponeIfOpen", CareTask.class);
    }

    private void assertOpenStatePredicate(String methodName, Class<?> parameterType)
            throws NoSuchMethodException {
        Method method = CareTaskMapper.class.getMethod(methodName, parameterType);
        String sql = String.join(" ", method.getAnnotation(Update.class).value()).toLowerCase();

        assertThat(sql).contains("where id = #{id}", "status in (0, 3)");
    }
}
