package dto;

import java.io.Serializable;
import java.util.Objects;

public class Page implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int id;
    private int bookId;
    private int pageNumber;
    private String content;
   
    public Page() {}

    public Page(int id, int bookId, int pageNumber, String content) {
        this.id = id;
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", pageNumber=" + pageNumber +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page)) return false;
        Page page = (Page) o;
        return id == page.id && bookId == page.bookId && pageNumber == page.pageNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookId, pageNumber);
    }
}
