drop table if exists TB_BOOKS;

create table TB_BOOKS(
    id long auto_increment not null primary key,
    title varchar(100) not null,
    isbn varchar(100) not null
)