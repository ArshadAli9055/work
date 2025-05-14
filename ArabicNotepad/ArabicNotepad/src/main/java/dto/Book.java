package dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Book implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String hash;
    private String idauthor;
    private List<Page> pages;

    public Book() {}

    public Book(int id, String title, String hash, String idauthor, List<Page> pages) {
        this.id = id;
        this.title = title;
        this.hash = hash;
        this.idauthor = idauthor;
        this.pages = pages;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getIdauthor() {
        return idauthor;
    }

    public void setIdauthor(String idauthor) {
        this.idauthor = idauthor;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", hash='" + hash + '\'' +
                ", idauthor='" + idauthor + '\'' +
                ", pages=" + (pages != null ? pages.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return id == book.id && Objects.equals(title, book.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }
}
