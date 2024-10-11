package com.example.myapp.controller;

import com.example.myapp.entity.Article;
import com.example.myapp.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
public class MyController {

    private final ArticleRepository articleRepository;

    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String formattedDateTime = now.format(formatter);


    @GetMapping("/add") // 기사 추가 페이지로 이동
    public String showAddArticleForm(Model model) {
        model.addAttribute("article", new Article()); // 새로운 Article 객체 추가
        return "addArticle";  // resources/templates/addArticle.html 파일을 반환
    }

    @PostMapping("/add") // 기사를 추가하는 메서드
    public String addArticle(@ModelAttribute Article article) {
        articleRepository.save(article); // 기사를 데이터베이스에 저장
        System.out.println(formattedDateTime + " article 추가: " + article.getTitle()  + article.getAuthor() + article.getContent());
        return "redirect:/add";
    }
}
