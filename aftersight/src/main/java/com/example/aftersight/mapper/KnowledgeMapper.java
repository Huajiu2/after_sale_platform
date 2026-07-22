package com.example.aftersight.mapper;

import com.example.aftersight.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface KnowledgeMapper {

    @Insert("INSERT INTO knowledge_doc (doc_code, doc_name, category, file_type, file_size, chunk_count, " +
            "vectorize_status, uploaded_by, uploaded_at) " +
            "VALUES (#{docCode}, #{docName}, #{category}, #{fileType}, #{fileSize}, #{chunkCount}, " +
            "#{vectorizeStatus}, #{uploadedBy}, #{uploadedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(KnowledgeDoc doc);

    @Select("SELECT CONCAT('DOC', LPAD(IFNULL(MAX(CAST(SUBSTRING(doc_code, 4) AS UNSIGNED)), 0) + 1, 3, '0')) FROM knowledge_doc")
    String nextDocCode();

    List<KnowledgeDoc> selectList(@Param("docName") String docName);

    @Select("SELECT * FROM knowledge_doc WHERE id = #{id}")
    KnowledgeDoc selectById(Long id);

    @Update("UPDATE knowledge_doc SET vectorize_status = #{vectorizeStatus}, chunk_count = #{chunkCount} WHERE id = #{id}")
    void updateStatus(KnowledgeDoc kd);

    @Delete("DELETE FROM knowledge_doc WHERE id = #{id}")
    void deleteById(Long id);
}
