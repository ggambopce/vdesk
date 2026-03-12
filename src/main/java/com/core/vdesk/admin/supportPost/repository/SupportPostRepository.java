package com.core.vdesk.admin.supportPost.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.core.vdesk.admin.supportPost.entity.BoardType;
import com.core.vdesk.admin.supportPost.entity.SupportPosts;

public interface SupportPostRepository extends JpaRepository<SupportPosts, Long> {

    Optional<SupportPosts> findByIdAndDeletedFalse(Long id);

    @Query("""
        select p from SupportPosts p
        where p.deleted = false
          and p.type = :type
          and (:q is null or ( :field = 'ALL' and (p.title like %:q% or p.content like %:q%) )
               or ( :field = 'TITLE' and p.title like %:q% )
               or ( :field = 'CONTENT' and p.content like %:q% )
          )
    """)
    Page<SupportPosts> search(@Param("type") BoardType type,
                               @Param("q") String q,
                               @Param("field") String field,
                               Pageable pageable);

    @Query("""
        select p from SupportPosts p
        where p.deleted = false
          and p.type = :type
          and p.author.userId = :userId
          and (:q is null or ( :field = 'ALL' and (p.title like %:q% or p.content like %:q%) )
               or ( :field = 'TITLE' and p.title like %:q% )
               or ( :field = 'CONTENT' and p.content like %:q% )
          )
    """)
    Page<SupportPosts> searchMine(@Param("type") BoardType type,
                                   @Param("userId") Long userId,
                                   @Param("q") String q,
                                   @Param("field") String field,
                                   Pageable pageable);
}

