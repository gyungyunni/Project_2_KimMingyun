package com.example.mutsasns.service;

import com.example.mutsasns.dto.article.ArticlePageDto;
import com.example.mutsasns.dto.article.ArticleReadDto;
import com.example.mutsasns.dto.article.ArticleRequestDto;
import com.example.mutsasns.dto.article.ArticleUpdateDto;
import com.example.mutsasns.entity.ArticleEntity;
import com.example.mutsasns.entity.ArticleImagesEntity;
import com.example.mutsasns.entity.UserEntity;
import com.example.mutsasns.repository.ArticleImagesRepository;
import com.example.mutsasns.repository.ArticleRepository;
import com.example.mutsasns.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleImagesRepository articleImagesRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ArticleRequestDto createArticle(ArticleRequestDto dto){

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);

        if(optionalUser.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        UserEntity userEntity = optionalUser.get();

        List<ArticleImagesEntity> imagesList = new ArrayList<>();

        ArticleEntity article = new ArticleEntity();
        article.setUser(userEntity);
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setArticleImages(imagesList);
        articleRepository.save(article);

        if(!(dto.getImageList() == null)){

            String articleImageDir = String.format("media/article/%d/", article.getId());
            try {
                Files.createDirectories(Path.of(articleImageDir));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            int i = 1;

            for(MultipartFile img : dto.getImageList()) {

                ArticleImagesEntity articleImages = new ArticleImagesEntity();

                // 확장자를 포함한 이미지 이름 만들기
                String originalFilename = img.getOriginalFilename();
                String[] fileNameSplit = originalFilename.split("\\.");
                String extension = fileNameSplit[fileNameSplit.length - 1];
                String articleFilename = username + "_" + i + "." + extension;
                i ++; // i를 증가시켜 다음 이미지에 대한 파일 이름 생성

                // 폴더와 파일 경로를 포함한 이름 만들기
                String articleImagePath = articleImageDir + articleFilename;

                // MultipartFile 을 저장하기
                try {
                    img.transferTo(Path.of(articleImagePath));
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                articleImages.setImage_url(String.format("/static/article/%d/%s", article.getId(), articleFilename));
                articleImages.setArticle(article);
                articleImagesRepository.save(articleImages);
            }

            // 모든 이미지가 저장된 후에 ArticleEntity를 한 번만 저장
            articleRepository.save(article);
        }

        return ArticleRequestDto.fromEntity(article);
    }

    public Page<ArticlePageDto> readArticlePage(Integer pageNumber, Integer pageSize) {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);

        if(optionalUser.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        UserEntity userEntity = optionalUser.get();

        Pageable pageable = PageRequest.of(
                pageNumber, pageSize, Sort.by("id").ascending());

        Page<ArticleEntity> ArticlePage
                = articleRepository.findAllByUser(userEntity, pageable);

        Page<ArticlePageDto> ArticleDtoPage
                = ArticlePage.map(ArticlePageDto::fromEntity);

        return ArticleDtoPage;
    }

    public ArticleReadDto readArticle(Long id){

        Optional<ArticleEntity> optionalArticle
                = articleRepository.findById(id);

        if(optionalArticle.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        return ArticleReadDto.fromEntity(optionalArticle.get());

    }

    // update content or title or 이미지 삭제
    public void updateArticle(ArticleUpdateDto dto, Long articleId){
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);

        if(optionalUser.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        UserEntity userEntity = optionalUser.get();

        Optional<ArticleEntity> optionalArticle = articleRepository.findByIdAndUser(articleId, userEntity);

        if (optionalArticle.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ArticleEntity article = optionalArticle.get();

        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());

        int lastImageIndex = article.getArticleImages().size();

        if(!(dto.getUpdateImageList() == null))
            for(MultipartFile img : dto.getUpdateImageList()){
                String postImageDir = String.format("media/article/%d/", article.getId());

                try {
                    Files.createDirectories(Path.of(postImageDir));
                } catch (IOException e) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                // 확장자를 포함한 이미지 이름 만들기
                String originalFilename = img.getOriginalFilename();
                String[] fileNameSplit = originalFilename.split("\\.");
                String extension = fileNameSplit[fileNameSplit.length - 1];
                String articleFilename = username + "_" + lastImageIndex + "." + extension;
                lastImageIndex ++; // lastImageIndex를 증가시켜 다음 이미지에 대한 파일 이름 생성


                String postImagePath = postImageDir + articleFilename;
                try {
                    img.transferTo(Path.of(postImagePath));
                } catch (IOException e) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                ArticleImagesEntity images = new ArticleImagesEntity();
                images.setImage_url(String.format("/static/article/%d/%s", article.getId(), articleFilename));
                images.setArticle(article);
                articleImagesRepository.save(images);
                article.getArticleImages().add(images);
            }

        articleRepository.save(article);


    }
    public void deleteImage(Long articleId, Long imageId) {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        UserEntity userEntity = optionalUser.get();

        Optional<ArticleEntity> optionalArticle = articleRepository.findByIdAndUser(articleId, userEntity);

        if (optionalArticle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ArticleEntity article = optionalArticle.get();


        Optional<ArticleImagesEntity> articleImages = articleImagesRepository.findById(imageId);
        if (articleImages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ArticleImagesEntity articleImage = articleImages.get();

        if (articleId != articleImage.getArticle().getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String[] split = articleImage.getImage_url().split("/");
        String name = split[split.length - 1];
        String imagePath = "media/article/" + articleId + "/" + name;

        // 실제 서버에서 이미지 삭제
        try {
            Files.delete(Path.of(imagePath));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        articleImagesRepository.deleteById(imageId);

    }

    public boolean deleteArticle(Long id) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);

        if(optionalUser.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        UserEntity userEntity = optionalUser.get();

        Optional<ArticleEntity> optionalArticle = articleRepository.findByIdAndUser(id, userEntity);

        if (optionalArticle.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        ArticleEntity article = optionalArticle.get();
        article.setDeleted(true); //이렇게 하면 DB에는 값이 존재하지만, 로직 상에는 출력이 되지 않도록 할 수 있음.
                                  // 엔티티에 @Where(clause = "deleted = false")를 붙여놨기 때문

        article.setDeletedAt(DATE_TIME_FORMATTER.format(LocalDateTime.now()));

        articleRepository.save(article);

        articleRepository.findAll().forEach(System.out::println); // 로직상에 출력이 되는지 안되는지 test용
        return true;

    }
}
