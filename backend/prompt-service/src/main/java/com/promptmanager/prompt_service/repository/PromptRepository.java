package com.promptmanager.prompt_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.promptmanager.prompt_service.entity.Prompt;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {

    List<Prompt> findByTitleContainingIgnoreCase(String title);

    List<Prompt> findByCategoryIgnoreCase(String category);

}