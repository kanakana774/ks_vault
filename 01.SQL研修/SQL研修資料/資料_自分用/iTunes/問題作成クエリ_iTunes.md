下記単元の問題をそれぞれ 15 問ずつ用意したいです。
復習目的なので難易度は普通くらいで、多少単元の内容を飛び越えてもよいこととします。

一日 15 ～ 20 問ほど用意しました。
難易度は普通です。

総復習 1.データベースの検索/登録/更新/削除/関数
総復習 2.case 式/集計関数と group by/DDL
総復習 3.結合/集合演算子/サブクエリ
総復習 4.plpgsql/ウィンドウ関数

使用テーブルは下記になります。
データも用意してるので別途必要ありません。

```SQL
/*******************************************************************************
   Create Tables
********************************************************************************/
CREATE TABLE album
(
    album_id SERIAL NOT NULL,
    title VARCHAR(160) NOT NULL,
    artist_id INT NOT NULL,
    CONSTRAINT album_pkey PRIMARY KEY  (album_id)
);

CREATE TABLE artist
(
    artist_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT artist_pkey PRIMARY KEY  (artist_id)
);

CREATE TABLE customer
(
    customer_id SERIAL NOT NULL,
    first_name VARCHAR(40) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    company VARCHAR(80),
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60) NOT NULL,
    support_rep_id INT,
    CONSTRAINT customer_pkey PRIMARY KEY  (customer_id)
);

CREATE TABLE employee
(
    employee_id SERIAL NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    title VARCHAR(30),
    reports_to INT,
    birth_date TIMESTAMP,
    hire_date TIMESTAMP,
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60),
    CONSTRAINT employee_pkey PRIMARY KEY  (employee_id)
);

CREATE TABLE genre
(
    genre_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT genre_pkey PRIMARY KEY  (genre_id)
);

CREATE TABLE invoice
(
    invoice_id SERIAL NOT NULL,
    customer_id INT NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    billing_address VARCHAR(70),
    billing_city VARCHAR(40),
    billing_state VARCHAR(40),
    billing_country VARCHAR(40),
    billing_postal_code VARCHAR(10),
    total NUMERIC(10,2) NOT NULL,
    CONSTRAINT invoice_pkey PRIMARY KEY  (invoice_id)
);

CREATE TABLE invoice_line
(
    invoice_line_id SERIAL NOT NULL,
    invoice_id INT NOT NULL,
    track_id INT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT invoice_line_pkey PRIMARY KEY  (invoice_line_id)
);

CREATE TABLE media_type
(
    media_type_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT media_type_pkey PRIMARY KEY  (media_type_id)
);

CREATE TABLE playlist
(
    playlist_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT playlist_pkey PRIMARY KEY  (playlist_id)
);

CREATE TABLE playlist_track
(
    playlist_id INT NOT NULL,
    track_id INT NOT NULL,
    CONSTRAINT playlist_track_pkey PRIMARY KEY  (playlist_id, track_id)
);

CREATE TABLE track
(
    track_id SERIAL NOT NULL,
    name VARCHAR(200) NOT NULL,
    album_id INT,
    media_type_id INT NOT NULL,
    genre_id INT,
    composer VARCHAR(220),
    milliseconds INT NOT NULL,
    bytes INT,
    unit_price NUMERIC(10,2) NOT NULL,
    CONSTRAINT track_pkey PRIMARY KEY  (track_id)
);



/*******************************************************************************
   Create Primary Key Unique Indexes
********************************************************************************/

/*******************************************************************************
   Create Foreign Keys
********************************************************************************/
ALTER TABLE album ADD CONSTRAINT album_artist_id_fkey
    FOREIGN KEY (artist_id) REFERENCES artist (artist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX album_artist_id_idx ON album (artist_id);

ALTER TABLE customer ADD CONSTRAINT customer_support_rep_id_fkey
    FOREIGN KEY (support_rep_id) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX customer_support_rep_id_idx ON customer (support_rep_id);

ALTER TABLE employee ADD CONSTRAINT employee_reports_to_fkey
    FOREIGN KEY (reports_to) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX employee_reports_to_idx ON employee (reports_to);

ALTER TABLE invoice ADD CONSTRAINT invoice_customer_id_fkey
    FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_customer_id_idx ON invoice (customer_id);

ALTER TABLE invoice_line ADD CONSTRAINT invoice_line_invoice_id_fkey
    FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_line_invoice_id_idx ON invoice_line (invoice_id);

ALTER TABLE invoice_line ADD CONSTRAINT invoice_line_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_line_track_id_idx ON invoice_line (track_id);

ALTER TABLE playlist_track ADD CONSTRAINT playlist_track_playlist_id_fkey
    FOREIGN KEY (playlist_id) REFERENCES playlist (playlist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX playlist_track_playlist_id_idx ON playlist_track (playlist_id);

ALTER TABLE playlist_track ADD CONSTRAINT playlist_track_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX playlist_track_track_id_idx ON playlist_track (track_id);

ALTER TABLE track ADD CONSTRAINT track_album_id_fkey
    FOREIGN KEY (album_id) REFERENCES album (album_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_album_id_idx ON track (album_id);

ALTER TABLE track ADD CONSTRAINT track_genre_id_fkey
    FOREIGN KEY (genre_id) REFERENCES genre (genre_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_genre_id_idx ON track (genre_id);

ALTER TABLE track ADD CONSTRAINT track_media_type_id_fkey
    FOREIGN KEY (media_type_id) REFERENCES media_type (media_type_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_media_type_id_idx ON track (media_type_id);
```

データの一部は下記になります。

```SQL
/*******************************************************************************
   Populate Tables
********************************************************************************/

INSERT INTO genre (name) VALUES
    (N'Rock'),
    (N'Jazz'),
    (N'Metal'),
    (N'Alternative & Punk'),
    (N'Rock And Roll'),
    (N'Blues'),
    (N'Latin'),
    (N'Reggae'),
    (N'Pop'),
    (N'Soundtrack'),
    (N'Bossa Nova'),
    (N'Easy Listening'),
    (N'Heavy Metal'),
    (N'R&B/Soul'),
    (N'Electronica/Dance'),
    (N'World'),
    (N'Hip Hop/Rap'),
    (N'Science Fiction'),
    (N'TV Shows'),
    (N'Sci Fi & Fantasy'),
    (N'Drama'),
    (N'Comedy'),
    (N'Alternative'),
    (N'Classical'),
    (N'Opera');

INSERT INTO media_type (name) VALUES
    (N'MPEG audio file'),
    (N'Protected AAC audio file'),
    (N'Protected MPEG-4 video file'),
    (N'Purchased AAC audio file'),
    (N'AAC audio file');

INSERT INTO artist (name) VALUES
    (N'AC/DC'),
    (N'Accept'),
    (N'Aerosmith'),
    (N'Alanis Morissette'),
    (N'Alice In Chains'),
    (N'Antônio Carlos Jobim'),
    (N'Apocalyptica'),
    (N'Audioslave'),
    (N'BackBeat'),
    (N'Billy Cobham'),
    (N'Black Label Society'),
    (N'Black Sabbath'),
    (N'Body Count'),
    (N'Bruce Dickinson'),
    (N'Buddy Guy'),
    (N'Caetano Veloso'),
    (N'Chico Buarque'),
    (N'Chico Science & Nação Zumbi'),
    (N'Cidade Negra'),
    (N'Cláudio Zoli'),
    (N'Various Artists'),
    (N'Led Zeppelin'),
    (N'Frank Zappa & Captain Beefheart'),
    (N'Marcos Valle'),
    (N'Milton Nascimento & Bebeto'),
    (N'Azymuth'),
    (N'Gilberto Gil'),
    (N'João Gilberto'),
    (N'Bebel Gilberto'),
    (N'Jorge Vercilo'),
    (N'Baby Consuelo'),
    (N'Ney Matogrosso'),
    (N'Luiz Melodia'),
    (N'Nando Reis'),
    (N'Pedro Luís & A Parede'),
    (N'O Rappa'),
    (N'Ed Motta'),
    (N'Banda Black Rio'),
    (N'Fernanda Porto'),
    (N'Os Cariocas'),
    (N'Elis Regina'),
    (N'Milton Nascimento'),・・・

INSERT INTO album (title, artist_id) VALUES
    (N'For Those About To Rock We Salute You', 1),
    (N'Balls to the Wall', 2),
    (N'Restless and Wild', 2),
    (N'Let There Be Rock', 1),
    (N'Big Ones', 3),
    (N'Jagged Little Pill', 4),
    (N'Facelift', 5),
    (N'Warner 25 Anos', 6),
    (N'Plays Metallica By Four Cellos', 7),
    (N'Audioslave', 8),
    (N'Out Of Exile', 8),
    (N'BackBeat Soundtrack', 9),
    (N'The Best Of Billy Cobham', 10),
    (N'Alcohol Fueled Brewtality Live! [Disc 1]', 11),
    (N'Alcohol Fueled Brewtality Live! [Disc 2]', 11),
    (N'Black Sabbath', 12),
    (N'Black Sabbath Vol. 4 (Remaster)', 12),
    (N'Body Count', 13),
    (N'Chemical Wedding', 14),
    (N'The Best Of Buddy Guy - The Millenium Collection', 15),
    (N'Prenda Minha', 16),
    (N'Sozinho Remix Ao Vivo', 16),
    (N'Minha Historia', 17),
    (N'Afrociberdelia', 18),
    (N'Da Lama Ao Caos', 18),
    (N'Acústico MTV [Live]', 19),
    (N'Cidade Negra - Hits', 19),
    (N'Na Pista', 20),
    (N'Axé Bahia 2001', 21),
    (N'BBC Sessions [Disc 1] [Live]', 22),
    (N'Bongo Fury', 23),
    (N'Carnaval 2001', 21),
    (N'Chill: Brazil (Disc 1)', 24),
    (N'Chill: Brazil (Disc 2)', 6),・・・

INSERT INTO track (name, album_id, media_type_id, genre_id, composer, milliseconds, bytes, unit_price) VALUES
    (N'For Those About To Rock (We Salute You)', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 343719, 11170334, 0.99),
    (N'Balls to the Wall', 2, 2, 1, N'U. Dirkschneider, W. Hoffmann, H. Frank, P. Baltes, S. Kaufmann, G. Hoffmann', 342562, 5510424, 0.99),
    (N'Fast As a Shark', 3, 2, 1, N'F. Baltes, S. Kaufman, U. Dirkscneider & W. Hoffman', 230619, 3990994, 0.99),
    (N'Restless and Wild', 3, 2, 1, N'F. Baltes, R.A. Smith-Diesel, S. Kaufman, U. Dirkscneider & W. Hoffman', 252051, 4331779, 0.99),
    (N'Princess of the Dawn', 3, 2, 1, N'Deaffy & R.A. Smith-Diesel', 375418, 6290521, 0.99),
    (N'Put The Finger On You', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 205662, 6713451, 0.99),
    (N'Let''s Get It Up', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 233926, 7636561, 0.99),
    (N'Inject The Venom', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 210834, 6852860, 0.99),
    (N'Snowballed', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 203102, 6599424, 0.99),
    (N'Evil Walks', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 263497, 8611245, 0.99),
    (N'C.O.D.', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 199836, 6566314, 0.99),
    (N'Breaking The Rules', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 263288, 8596840, 0.99),
    (N'Night Of The Long Knives', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 205688, 6706347, 0.99),
    (N'Spellbound', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 270863, 8817038, 0.99),
    (N'Go Down', 4, 1, 1, N'AC/DC', 331180, 10847611, 0.99),
    (N'Dog Eat Dog', 4, 1, 1, N'AC/DC', 215196, 7032162, 0.99),
    (N'Let There Be Rock', 4, 1, 1, N'AC/DC', 366654, 12021261, 0.99),
    (N'Bad Boy Boogie', 4, 1, 1, N'AC/DC', 267728, 8776140, 0.99),
    (N'Problem Child', 4, 1, 1, N'AC/DC', 325041, 10617116, 0.99),
    (N'Overdose', 4, 1, 1, N'AC/DC', 369319, 12066294, 0.99),・・・

INSERT INTO employee (last_name, first_name, title, reports_to, birth_date, hire_date, address, city, state, country, postal_code, phone, fax, email) VALUES
    (N'Adams', N'Andrew', N'General Manager', NULL, '1962/2/18', '2002/8/14', N'11120 Jasper Ave NW', N'Edmonton', N'AB', N'Canada', N'T5K 2N1', N'+1 (780) 428-9482', N'+1 (780) 428-3457', N'andrew@chinookcorp.com'),
    (N'Edwards', N'Nancy', N'Sales Manager', 1, '1958/12/8', '2002/5/1', N'825 8 Ave SW', N'Calgary', N'AB', N'Canada', N'T2P 2T3', N'+1 (403) 262-3443', N'+1 (403) 262-3322', N'nancy@chinookcorp.com'),
    (N'Peacock', N'Jane', N'Sales Support Agent', 2, '1973/8/29', '2002/4/1', N'1111 6 Ave SW', N'Calgary', N'AB', N'Canada', N'T2P 5M5', N'+1 (403) 262-3443', N'+1 (403) 262-6712', N'jane@chinookcorp.com'),
    (N'Park', N'Margaret', N'Sales Support Agent', 2, '1947/9/19', '2003/5/3', N'683 10 Street SW', N'Calgary', N'AB', N'Canada', N'T2P 5G3', N'+1 (403) 263-4423', N'+1 (403) 263-4289', N'margaret@chinookcorp.com'),
    (N'Johnson', N'Steve', N'Sales Support Agent', 2, '1965/3/3', '2003/10/17', N'7727B 41 Ave', N'Calgary', N'AB', N'Canada', N'T3B 1Y7', N'1 (780) 836-9987', N'1 (780) 836-9543', N'steve@chinookcorp.com'),
    (N'Mitchell', N'Michael', N'IT Manager', 1, '1973/7/1', '2003/10/17', N'5827 Bowness Road NW', N'Calgary', N'AB', N'Canada', N'T3B 0C5', N'+1 (403) 246-9887', N'+1 (403) 246-9899', N'michael@chinookcorp.com'),
    (N'King', N'Robert', N'IT Staff', 6, '1970/5/29', '2004/1/2', N'590 Columbia Boulevard West', N'Lethbridge', N'AB', N'Canada', N'T1K 5N8', N'+1 (403) 456-9986', N'+1 (403) 456-8485', N'robert@chinookcorp.com'),
    (N'Callahan', N'Laura', N'IT Staff', 6, '1968/1/9', '2004/3/4', N'923 7 ST NW', N'Lethbridge', N'AB', N'Canada', N'T1H 1Y8', N'+1 (403) 467-3351', N'+1 (403) 467-8772', N'laura@chinookcorp.com');

INSERT INTO customer (first_name, last_name, company, address, city, state, country, postal_code, phone, fax, email, support_rep_id) VALUES
    (N'Luís', N'Gonçalves', N'Embraer - Empresa Brasileira de Aeronáutica S.A.', N'Av. Brigadeiro Faria Lima, 2170', N'São José dos Campos', N'SP', N'Brazil', N'12227-000', N'+55 (12) 3923-5555', N'+55 (12) 3923-5566', N'luisg@embraer.com.br', 3),
    (N'Leonie', N'Köhler', NULL, N'Theodor-Heuss-Straße 34', N'Stuttgart', NULL, N'Germany', N'70174', N'+49 0711 2842222', NULL, N'leonekohler@surfeu.de', 5),
    (N'François', N'Tremblay', NULL, N'1498 rue Bélanger', N'Montréal', N'QC', N'Canada', N'H2G 1A7', N'+1 (514) 721-4711', NULL, N'ftremblay@gmail.com', 3),
    (N'Bjørn', N'Hansen', NULL, N'Ullevålsveien 14', N'Oslo', NULL, N'Norway', N'0171', N'+47 22 44 22 22', NULL, N'bjorn.hansen@yahoo.no', 4),
    (N'František', N'Wichterlová', N'JetBrains s.r.o.', N'Klanova 9/506', N'Prague', NULL, N'Czech Republic', N'14700', N'+420 2 4172 5555', N'+420 2 4172 5555', N'frantisekw@jetbrains.com', 4),
    (N'Helena', N'Holý', NULL, N'Rilská 3174/6', N'Prague', NULL, N'Czech Republic', N'14300', N'+420 2 4177 0449', NULL, N'hholy@gmail.com', 5),
    (N'Astrid', N'Gruber', NULL, N'Rotenturmstraße 4, 1010 Innere Stadt', N'Vienne', NULL, N'Austria', N'1010', N'+43 01 5134505', NULL, N'astrid.gruber@apple.at', 5),
    (N'Daan', N'Peeters', NULL, N'Grétrystraat 63', N'Brussels', NULL, N'Belgium', N'1000', N'+32 02 219 03 03', NULL, N'daan_peeters@apple.be', 4),
    (N'Kara', N'Nielsen', NULL, N'Sønder Boulevard 51', N'Copenhagen', NULL, N'Denmark', N'1720', N'+453 3331 9991', NULL, N'kara.nielsen@jubii.dk', 4),
    (N'Eduardo', N'Martins', N'Woodstock Discos', N'Rua Dr. Falcão Filho, 155', N'São Paulo', N'SP', N'Brazil', N'01007-010', N'+55 (11) 3033-5446', N'+55 (11) 3033-4564', N'eduardo@woodstock.com.br', 4),
    (N'Alexandre', N'Rocha', N'Banco do Brasil S.A.', N'Av. Paulista, 2022', N'São Paulo', N'SP', N'Brazil', N'01310-200', N'+55 (11) 3055-3278', N'+55 (11) 3055-8131', N'alero@uol.com.br', 5),
    (N'Roberto', N'Almeida', N'Riotur', N'Praça Pio X, 119', N'Rio de Janeiro', N'RJ', N'Brazil', N'20040-020', N'+55 (21) 2271-7000', N'+55 (21) 2271-7070', N'roberto.almeida@riotur.gov.br', 3),
    (N'Fernanda', N'Ramos', NULL, N'Qe 7 Bloco G', N'Brasília', N'DF', N'Brazil', N'71020-677', N'+55 (61) 3363-5547', N'+55 (61) 3363-7855', N'fernadaramos4@uol.com.br', 4),
    (N'Mark', N'Philips', N'Telus', N'8210 111 ST NW', N'Edmonton', N'AB', N'Canada', N'T6G 2C7', N'+1 (780) 434-4554', N'+1 (780) 434-5565', N'mphilips12@shaw.ca', 5),
    (N'Jennifer', N'Peterson', N'Rogers Canada', N'700 W Pender Street', N'Vancouver', N'BC', N'Canada', N'V6C 1G8', N'+1 (604) 688-2255', N'+1 (604) 688-8756', N'jenniferp@rogers.ca', 3),
    (N'Frank', N'Harris', N'Google Inc.', N'1600 Amphitheatre Parkway', N'Mountain View', N'CA', N'USA', N'94043-1351', N'+1 (650) 253-0000', N'+1 (650) 253-0000', N'fharris@google.com', 4),
    (N'Jack', N'Smith', N'Microsoft Corporation', N'1 Microsoft Way', N'Redmond', N'WA', N'USA', N'98052-8300', N'+1 (425) 882-8080', N'+1 (425) 882-8081', N'jacksmith@microsoft.com', 5),
    (N'Michelle', N'Brooks', NULL, N'627 Broadway', N'New York', N'NY', N'USA', N'10012-2612', N'+1 (212) 221-3546', N'+1 (212) 221-4679', N'michelleb@aol.com', 3),
    (N'Tim', N'Goyer', N'Apple Inc.', N'1 Infinite Loop', N'Cupertino', N'CA', N'USA', N'95014', N'+1 (408) 996-1010', N'+1 (408) 996-1011', N'tgoyer@apple.com', 3),
    (N'Dan', N'Miller', NULL, N'541 Del Medio Avenue', N'Mountain View', N'CA', N'USA', N'94040-111', N'+1 (650) 644-3358', NULL, N'dmiller@comcast.com', 4),
    (N'Kathy', N'Chase', NULL, N'801 W 4th Street', N'Reno', N'NV', N'USA', N'89503', N'+1 (775) 223-7665', NULL, N'kachase@hotmail.com', 5),
    (N'Heather', N'Leacock', NULL, N'120 S Orange Ave', N'Orlando', N'FL', N'USA', N'32801', N'+1 (407) 999-7788', NULL, N'hleacock@gmail.com', 4),
    (N'John', N'Gordon', NULL, N'69 Salem Street', N'Boston', N'MA', N'USA', N'2113', N'+1 (617) 522-1333', NULL, N'johngordon22@yahoo.com', 4),
    (N'Frank', N'Ralston', NULL, N'162 E Superior Street', N'Chicago', N'IL', N'USA', N'60611', N'+1 (312) 332-3232', NULL, N'fralston@gmail.com', 3),
    (N'Victor', N'Stevens', NULL, N'319 N. Frances Street', N'Madison', N'WI', N'USA', N'53703', N'+1 (608) 257-0597', NULL, N'vstevens@yahoo.com', 5),
    (N'Richard', N'Cunningham', NULL, N'2211 W Berry Street', N'Fort Worth', N'TX', N'USA', N'76110', N'+1 (817) 924-7272', NULL, N'ricunningham@hotmail.com', 4),
    (N'Patrick', N'Gray', NULL, N'1033 N Park Ave', N'Tucson', N'AZ', N'USA', N'85719', N'+1 (520) 622-4200', NULL, N'patrick.gray@aol.com', 4),
    (N'Julia', N'Barnett', NULL, N'302 S 700 E', N'Salt Lake City', N'UT', N'USA', N'84102', N'+1 (801) 531-7272', NULL, N'jubarnett@gmail.com', 5),
    (N'Robert', N'Brown', NULL, N'796 Dundas Street West', N'Toronto', N'ON', N'Canada', N'M6J 1V1', N'+1 (416) 363-8888', NULL, N'robbrown@shaw.ca', 3),
    (N'Edward', N'Francis', NULL, N'230 Elgin Street', N'Ottawa', N'ON', N'Canada', N'K2P 1L7', N'+1 (613) 234-3322', NULL, N'edfrancis@yachoo.ca', 3),
    (N'Martha', N'Silk', NULL, N'194A Chain Lake Drive', N'Halifax', N'NS', N'Canada', N'B3S 1C5', N'+1 (902) 450-0450', NULL, N'marthasilk@gmail.com', 5),
    (N'Aaron', N'Mitchell', NULL, N'696 Osborne Street', N'Winnipeg', N'MB', N'Canada', N'R3L 2B9', N'+1 (204) 452-6452', NULL, N'aaronmitchell@yahoo.ca', 4),
    (N'Ellie', N'Sullivan', NULL, N'5112 48 Street', N'Yellowknife', N'NT', N'Canada', N'X1A 1N6', N'+1 (867) 920-2233', NULL, N'ellie.sullivan@shaw.ca', 3),・・・

INSERT INTO invoice (customer_id, invoice_date, billing_address, billing_city, billing_state, billing_country, billing_postal_code, total) VALUES
    (2, '2021/1/1', N'Theodor-Heuss-Straße 34', N'Stuttgart', NULL, N'Germany', N'70174', 1.98),
    (4, '2021/1/2', N'Ullevålsveien 14', N'Oslo', NULL, N'Norway', N'0171', 3.96),
    (8, '2021/1/3', N'Grétrystraat 63', N'Brussels', NULL, N'Belgium', N'1000', 5.94),
    (14, '2021/1/6', N'8210 111 ST NW', N'Edmonton', N'AB', N'Canada', N'T6G 2C7', 8.91),
    (23, '2021/1/11', N'69 Salem Street', N'Boston', N'MA', N'USA', N'2113', 13.86),
    (37, '2021/1/19', N'Berger Straße 10', N'Frankfurt', NULL, N'Germany', N'60316', 0.99),
    (38, '2021/2/1', N'Barbarossastraße 19', N'Berlin', NULL, N'Germany', N'10779', 1.98),
    (40, '2021/2/1', N'8, Rue Hanovre', N'Paris', NULL, N'France', N'75002', 1.98),
    (42, '2021/2/2', N'9, Place Louis Barthou', N'Bordeaux', NULL, N'France', N'33000', 3.96),
    (46, '2021/2/3', N'3 Chatham Street', N'Dublin', N'Dublin', N'Ireland', NULL, 5.94),
    (52, '2021/2/6', N'202 Hoxton Street', N'London', NULL, N'United Kingdom', N'N1 5LH', 8.91),
    (2, '2021/2/11', N'Theodor-Heuss-Straße 34', N'Stuttgart', NULL, N'Germany', N'70174', 13.86),
    (16, '2021/2/19', N'1600 Amphitheatre Parkway', N'Mountain View', N'CA', N'USA', N'94043-1351', 0.99),
    (17, '2021/3/4', N'1 Microsoft Way', N'Redmond', N'WA', N'USA', N'98052-8300', 1.98),
    (19, '2021/3/4', N'1 Infinite Loop', N'Cupertino', N'CA', N'USA', N'95014', 1.98),
    (21, '2021/3/5', N'801 W 4th Street', N'Reno', N'NV', N'USA', N'89503', 3.96),
    (25, '2021/3/6', N'319 N. Frances Street', N'Madison', N'WI', N'USA', N'53703', 5.94),
    (31, '2021/3/9', N'194A Chain Lake Drive', N'Halifax', N'NS', N'Canada', N'B3S 1C5', 8.91),
    (40, '2021/3/14', N'8, Rue Hanovre', N'Paris', NULL, N'France', N'75002', 13.86),
    (54, '2021/3/22', N'110 Raeburn Pl', N'Edinburgh ', NULL, N'United Kingdom', N'EH4 1HH', 0.99),
    (55, '2021/4/4', N'421 Bourke Street', N'Sidney', N'NSW', N'Australia', N'2010', 1.98),
    (57, '2021/4/4', N'Calle Lira, 198', N'Santiago', NULL, N'Chile', NULL, 1.98),
    (59, '2021/4/5', N'3,Raj Bhavan Road', N'Bangalore', NULL, N'India', N'560001', 3.96),
    (4, '2021/4/6', N'Ullevålsveien 14', N'Oslo', NULL, N'Norway', N'0171', 5.94),
    (10, '2021/4/9', N'Rua Dr. Falcão Filho, 155', N'São Paulo', N'SP', N'Brazil', N'01007-010', 8.91),
    (19, '2021/4/14', N'1 Infinite Loop', N'Cupertino', N'CA', N'USA', N'95014', 13.86),
    (33, '2021/4/22', N'5112 48 Street', N'Yellowknife', N'NT', N'Canada', N'X1A 1N6', 0.99),
    (34, '2021/5/5', N'Rua da Assunção 53', N'Lisbon', NULL, N'Portugal', NULL, 1.98),
    (36, '2021/5/5', N'Tauentzienstraße 8', N'Berlin', NULL, N'Germany', N'10789', 1.98),
    (38, '2021/5/6', N'Barbarossastraße 19', N'Berlin', NULL, N'Germany', N'10779', 3.96),
    (42, '2021/5/7', N'9, Place Louis Barthou', N'Bordeaux', NULL, N'France', N'33000', 5.94),
    (48, '2021/5/10', N'Lijnbaansgracht 120bg', N'Amsterdam', N'VV', N'Netherlands', N'1016', 8.91),
    (57, '2021/5/15', N'Calle Lira, 198', N'Santiago', NULL, N'Chile', NULL, 13.86),
    (12, '2021/5/23', N'Praça Pio X, 119', N'Rio de Janeiro', N'RJ', N'Brazil', N'20040-020', 0.99),
    (13, '2021/6/5', N'Qe 7 Bloco G', N'Brasília', N'DF', N'Brazil', N'71020-677', 1.98),
    (15, '2021/6/5', N'700 W Pender Street', N'Vancouver', N'BC', N'Canada', N'V6C 1G8', 1.98),
    (17, '2021/6/6', N'1 Microsoft Way', N'Redmond', N'WA', N'USA', N'98052-8300', 3.96),
    (21, '2021/6/7', N'801 W 4th Street', N'Reno', N'NV', N'USA', N'89503', 5.94),
    (27, '2021/6/10', N'1033 N Park Ave', N'Tucson', N'AZ', N'USA', N'85719', 8.91),
    (36, '2021/6/15', N'Tauentzienstraße 8', N'Berlin', NULL, N'Germany', N'10789', 13.86),
    (50, '2021/6/23', N'C/ San Bernardo 85', N'Madrid', NULL, N'Spain', N'28015', 0.99),
    (51, '2021/7/6', N'Celsiusg. 9', N'Stockholm', NULL, N'Sweden', N'11230', 1.98),
    (53, '2021/7/6', N'113 Lupus St', N'London', NULL, N'United Kingdom', N'SW1V 3EN', 1.98),
    (55, '2021/7/7', N'421 Bourke Street', N'Sidney', N'NSW', N'Australia', N'2010', 3.96),
    (59, '2021/7/8', N'3,Raj Bhavan Road', N'Bangalore', NULL, N'India', N'560001', 5.94),
    (6, '2021/7/11', N'Rilská 3174/6', N'Prague', NULL, N'Czech Republic', N'14300', 8.91),
    (15, '2021/7/16', N'700 W Pender Street', N'Vancouver', N'BC', N'Canada', N'V6C 1G8', 13.86),・・・
INSERT INTO invoice_line (invoice_id, track_id, unit_price, quantity) VALUES
    (1, 2, 0.99, 1),
    (1, 4, 0.99, 1),
    (2, 6, 0.99, 1),
    (2, 8, 0.99, 1),
    (2, 10, 0.99, 1),
    (2, 12, 0.99, 1),
    (3, 16, 0.99, 1),
    (3, 20, 0.99, 1),
    (3, 24, 0.99, 1),
    (3, 28, 0.99, 1),
    (3, 32, 0.99, 1),
    (3, 36, 0.99, 1),
    (4, 42, 0.99, 1),
    (4, 48, 0.99, 1),
    (4, 54, 0.99, 1),
    (4, 60, 0.99, 1),
    (4, 66, 0.99, 1),
    (4, 72, 0.99, 1),・・・

INSERT INTO playlist (name) VALUES
    (N'Music'),
    (N'Movies'),
    (N'TV Shows'),
    (N'Audiobooks'),
    (N'90’s Music'),
    (N'Audiobooks'),
    (N'Movies'),
    (N'Music'),
    (N'Music Videos'),
    (N'TV Shows'),
    (N'Brazilian Music'),
    (N'Classical'),
    (N'Classical 101 - Deep Cuts'),
    (N'Classical 101 - Next Steps'),
    (N'Classical 101 - The Basics'),
    (N'Grunge'),
    (N'Heavy Metal Classic'),
    (N'On-The-Go 1');

INSERT INTO playlist_track (playlist_id, track_id) VALUES
    (1, 3402),
    (1, 3389),
    (1, 3390),
    (1, 3391),
    (1, 3392),
    (1, 3393),
    (1, 3394),
    (1, 3395),
    (1, 3396),
    (1, 3397),
    (1, 3398),
    (1, 3399),
    (1, 3400),
    (1, 3401),
    (1, 3336),
    (1, 3478),
    (1, 3375),
    (1, 3376),
    (1, 3377),
    (1, 3378),
    (1, 3379),
    (1, 3380),
    (1, 3381),・・・

```

まずは単元 1 についての問題を 15 問作成お願いします。
問題の趣旨は被らないようにしてほしいのと、必ず回答を付けてください。
また複数パターン解答があればそれも併せて出力してください。



---

postgresqlでgroupby、結合、サブクエリまでくらいを使用した複合問題を20問作成してほしいです。
難易度は普通くらいでお願いいたします。
問題と答えとヒントもあるといいかと思います。

下記のような業務内容を想定した問題内容にしていただき、各問題ごとにどんな業務を想定しているか記載お願いいたします。

## 業務システム系（最も多いパターン）

### 主な仕事
- 業務システム（販売管理・会計・物流など）の**データ抽出・加工・集計処理**をSQLで実装。
- **View、Stored Procedure、Function**を作成・改修。
- **夜間バッチ処理**や**帳票出力用SQL**の調整。
- **障害調査・原因分析**でSQLを用いてデータ追跡。

### よく使う技術
- SQL（Oracle / PostgreSQL / SQL Server / MySQL）
- PL/SQL（Oracle）、T-SQL（SQL Server）
- Shell / Python / JavaなどでのSQLバッチ実行
- BIツール連携（Tableau, Power BIなど）

### よくあるタスク例
| タスク | 内容 |
|--------|------|
| データ抽出 | 月次売上や支店別集計のクエリを作成 |
| 更新SQL | 条件に合致する顧客情報の一括更新 |
| SQLレビュー | クエリロジックや性能を検証 |
| チューニング | インデックス追加やEXPLAIN分析 |
| データ移行 | システム間データ変換・登録処理 |


テーブルとデータは下記を使います。データはすべて入りきらないので、一部です。


```SQL
/*******************************************************************************
   Create Tables
********************************************************************************/
CREATE TABLE album
(
    album_id SERIAL NOT NULL,
    title VARCHAR(160) NOT NULL,
    artist_id INT NOT NULL,
    CONSTRAINT album_pkey PRIMARY KEY  (album_id)
);

CREATE TABLE artist
(
    artist_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT artist_pkey PRIMARY KEY  (artist_id)
);

CREATE TABLE customer
(
    customer_id SERIAL NOT NULL,
    first_name VARCHAR(40) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    company VARCHAR(80),
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60) NOT NULL,
    support_rep_id INT,
    CONSTRAINT customer_pkey PRIMARY KEY  (customer_id)
);

CREATE TABLE employee
(
    employee_id SERIAL NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    title VARCHAR(30),
    reports_to INT,
    birth_date TIMESTAMP,
    hire_date TIMESTAMP,
    address VARCHAR(70),
    city VARCHAR(40),
    state VARCHAR(40),
    country VARCHAR(40),
    postal_code VARCHAR(10),
    phone VARCHAR(24),
    fax VARCHAR(24),
    email VARCHAR(60),
    CONSTRAINT employee_pkey PRIMARY KEY  (employee_id)
);

CREATE TABLE genre
(
    genre_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT genre_pkey PRIMARY KEY  (genre_id)
);

CREATE TABLE invoice
(
    invoice_id SERIAL NOT NULL,
    customer_id INT NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    billing_address VARCHAR(70),
    billing_city VARCHAR(40),
    billing_state VARCHAR(40),
    billing_country VARCHAR(40),
    billing_postal_code VARCHAR(10),
    total NUMERIC(10,2) NOT NULL,
    CONSTRAINT invoice_pkey PRIMARY KEY  (invoice_id)
);

CREATE TABLE invoice_line
(
    invoice_line_id SERIAL NOT NULL,
    invoice_id INT NOT NULL,
    track_id INT NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT invoice_line_pkey PRIMARY KEY  (invoice_line_id)
);

CREATE TABLE media_type
(
    media_type_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT media_type_pkey PRIMARY KEY  (media_type_id)
);

CREATE TABLE playlist
(
    playlist_id SERIAL NOT NULL,
    name VARCHAR(120),
    CONSTRAINT playlist_pkey PRIMARY KEY  (playlist_id)
);

CREATE TABLE playlist_track
(
    playlist_id INT NOT NULL,
    track_id INT NOT NULL,
    CONSTRAINT playlist_track_pkey PRIMARY KEY  (playlist_id, track_id)
);

CREATE TABLE track
(
    track_id SERIAL NOT NULL,
    name VARCHAR(200) NOT NULL,
    album_id INT,
    media_type_id INT NOT NULL,
    genre_id INT,
    composer VARCHAR(220),
    milliseconds INT NOT NULL,
    bytes INT,
    unit_price NUMERIC(10,2) NOT NULL,
    CONSTRAINT track_pkey PRIMARY KEY  (track_id)
);



/*******************************************************************************
   Create Primary Key Unique Indexes
********************************************************************************/

/*******************************************************************************
   Create Foreign Keys
********************************************************************************/
ALTER TABLE album ADD CONSTRAINT album_artist_id_fkey
    FOREIGN KEY (artist_id) REFERENCES artist (artist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX album_artist_id_idx ON album (artist_id);

ALTER TABLE customer ADD CONSTRAINT customer_support_rep_id_fkey
    FOREIGN KEY (support_rep_id) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX customer_support_rep_id_idx ON customer (support_rep_id);

ALTER TABLE employee ADD CONSTRAINT employee_reports_to_fkey
    FOREIGN KEY (reports_to) REFERENCES employee (employee_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX employee_reports_to_idx ON employee (reports_to);

ALTER TABLE invoice ADD CONSTRAINT invoice_customer_id_fkey
    FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_customer_id_idx ON invoice (customer_id);

ALTER TABLE invoice_line ADD CONSTRAINT invoice_line_invoice_id_fkey
    FOREIGN KEY (invoice_id) REFERENCES invoice (invoice_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_line_invoice_id_idx ON invoice_line (invoice_id);

ALTER TABLE invoice_line ADD CONSTRAINT invoice_line_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX invoice_line_track_id_idx ON invoice_line (track_id);

ALTER TABLE playlist_track ADD CONSTRAINT playlist_track_playlist_id_fkey
    FOREIGN KEY (playlist_id) REFERENCES playlist (playlist_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX playlist_track_playlist_id_idx ON playlist_track (playlist_id);

ALTER TABLE playlist_track ADD CONSTRAINT playlist_track_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES track (track_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX playlist_track_track_id_idx ON playlist_track (track_id);

ALTER TABLE track ADD CONSTRAINT track_album_id_fkey
    FOREIGN KEY (album_id) REFERENCES album (album_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_album_id_idx ON track (album_id);

ALTER TABLE track ADD CONSTRAINT track_genre_id_fkey
    FOREIGN KEY (genre_id) REFERENCES genre (genre_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_genre_id_idx ON track (genre_id);

ALTER TABLE track ADD CONSTRAINT track_media_type_id_fkey
    FOREIGN KEY (media_type_id) REFERENCES media_type (media_type_id) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX track_media_type_id_idx ON track (media_type_id);
```

データの一部は下記になります。

```SQL
/*******************************************************************************
   Populate Tables
********************************************************************************/

INSERT INTO genre (name) VALUES
    (N'Rock'),
    (N'Jazz'),
    (N'Metal'),
    (N'Alternative & Punk'),
    (N'Rock And Roll'),
    (N'Blues'),
    (N'Latin'),
    (N'Reggae'),
    (N'Pop'),
    (N'Soundtrack'),
    (N'Bossa Nova'),
    (N'Easy Listening'),
    (N'Heavy Metal'),
    (N'R&B/Soul'),
    (N'Electronica/Dance'),
    (N'World'),
    (N'Hip Hop/Rap'),
    (N'Science Fiction'),
    (N'TV Shows'),
    (N'Sci Fi & Fantasy'),
    (N'Drama'),
    (N'Comedy'),
    (N'Alternative'),
    (N'Classical'),
    (N'Opera');

INSERT INTO media_type (name) VALUES
    (N'MPEG audio file'),
    (N'Protected AAC audio file'),
    (N'Protected MPEG-4 video file'),
    (N'Purchased AAC audio file'),
    (N'AAC audio file');

INSERT INTO artist (name) VALUES
    (N'AC/DC'),
    (N'Accept'),
    (N'Aerosmith'),
    (N'Alanis Morissette'),
    (N'Alice In Chains'),
    (N'Antônio Carlos Jobim'),
    (N'Apocalyptica'),
    (N'Audioslave'),
    (N'BackBeat'),
    (N'Billy Cobham'),
    (N'Black Label Society'),
    (N'Black Sabbath'),
    (N'Body Count'),
    (N'Bruce Dickinson'),
    (N'Buddy Guy'),
    (N'Caetano Veloso'),
    (N'Chico Buarque'),
    (N'Chico Science & Nação Zumbi'),
    (N'Cidade Negra'),
    (N'Cláudio Zoli'),
    (N'Various Artists'),
    (N'Led Zeppelin'),
    (N'Frank Zappa & Captain Beefheart'),
    (N'Marcos Valle'),
    (N'Milton Nascimento & Bebeto'),
    (N'Azymuth'),
    (N'Gilberto Gil'),
    (N'João Gilberto'),
    (N'Bebel Gilberto'),
    (N'Jorge Vercilo'),
    (N'Baby Consuelo'),
    (N'Ney Matogrosso'),
    (N'Luiz Melodia'),
    (N'Nando Reis'),
    (N'Pedro Luís & A Parede'),
    (N'O Rappa'),
    (N'Ed Motta'),
    (N'Banda Black Rio'),
    (N'Fernanda Porto'),
    (N'Os Cariocas'),
    (N'Elis Regina'),
    (N'Milton Nascimento'),・・・

INSERT INTO album (title, artist_id) VALUES
    (N'For Those About To Rock We Salute You', 1),
    (N'Balls to the Wall', 2),
    (N'Restless and Wild', 2),
    (N'Let There Be Rock', 1),
    (N'Big Ones', 3),
    (N'Jagged Little Pill', 4),
    (N'Facelift', 5),
    (N'Warner 25 Anos', 6),
    (N'Plays Metallica By Four Cellos', 7),
    (N'Audioslave', 8),
    (N'Out Of Exile', 8),
    (N'BackBeat Soundtrack', 9),
    (N'The Best Of Billy Cobham', 10),
    (N'Alcohol Fueled Brewtality Live! [Disc 1]', 11),
    (N'Alcohol Fueled Brewtality Live! [Disc 2]', 11),
    (N'Black Sabbath', 12),
    (N'Black Sabbath Vol. 4 (Remaster)', 12),
    (N'Body Count', 13),
    (N'Chemical Wedding', 14),
    (N'The Best Of Buddy Guy - The Millenium Collection', 15),
    (N'Prenda Minha', 16),
    (N'Sozinho Remix Ao Vivo', 16),
    (N'Minha Historia', 17),
    (N'Afrociberdelia', 18),
    (N'Da Lama Ao Caos', 18),
    (N'Acústico MTV [Live]', 19),
    (N'Cidade Negra - Hits', 19),
    (N'Na Pista', 20),
    (N'Axé Bahia 2001', 21),
    (N'BBC Sessions [Disc 1] [Live]', 22),
    (N'Bongo Fury', 23),
    (N'Carnaval 2001', 21),
    (N'Chill: Brazil (Disc 1)', 24),
    (N'Chill: Brazil (Disc 2)', 6),・・・

INSERT INTO track (name, album_id, media_type_id, genre_id, composer, milliseconds, bytes, unit_price) VALUES
    (N'For Those About To Rock (We Salute You)', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 343719, 11170334, 0.99),
    (N'Balls to the Wall', 2, 2, 1, N'U. Dirkschneider, W. Hoffmann, H. Frank, P. Baltes, S. Kaufmann, G. Hoffmann', 342562, 5510424, 0.99),
    (N'Fast As a Shark', 3, 2, 1, N'F. Baltes, S. Kaufman, U. Dirkscneider & W. Hoffman', 230619, 3990994, 0.99),
    (N'Restless and Wild', 3, 2, 1, N'F. Baltes, R.A. Smith-Diesel, S. Kaufman, U. Dirkscneider & W. Hoffman', 252051, 4331779, 0.99),
    (N'Princess of the Dawn', 3, 2, 1, N'Deaffy & R.A. Smith-Diesel', 375418, 6290521, 0.99),
    (N'Put The Finger On You', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 205662, 6713451, 0.99),
    (N'Let''s Get It Up', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 233926, 7636561, 0.99),
    (N'Inject The Venom', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 210834, 6852860, 0.99),
    (N'Snowballed', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 203102, 6599424, 0.99),
    (N'Evil Walks', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 263497, 8611245, 0.99),
    (N'C.O.D.', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 199836, 6566314, 0.99),
    (N'Breaking The Rules', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 263288, 8596840, 0.99),
    (N'Night Of The Long Knives', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 205688, 6706347, 0.99),
    (N'Spellbound', 1, 1, 1, N'Angus Young, Malcolm Young, Brian Johnson', 270863, 8817038, 0.99),
    (N'Go Down', 4, 1, 1, N'AC/DC', 331180, 10847611, 0.99),
    (N'Dog Eat Dog', 4, 1, 1, N'AC/DC', 215196, 7032162, 0.99),
    (N'Let There Be Rock', 4, 1, 1, N'AC/DC', 366654, 12021261, 0.99),
    (N'Bad Boy Boogie', 4, 1, 1, N'AC/DC', 267728, 8776140, 0.99),
    (N'Problem Child', 4, 1, 1, N'AC/DC', 325041, 10617116, 0.99),
    (N'Overdose', 4, 1, 1, N'AC/DC', 369319, 12066294, 0.99),・・・

INSERT INTO employee (last_name, first_name, title, reports_to, birth_date, hire_date, address, city, state, country, postal_code, phone, fax, email) VALUES
    (N'Adams', N'Andrew', N'General Manager', NULL, '1962/2/18', '2002/8/14', N'11120 Jasper Ave NW', N'Edmonton', N'AB', N'Canada', N'T5K 2N1', N'+1 (780) 428-9482', N'+1 (780) 428-3457', N'andrew@chinookcorp.com'),
    (N'Edwards', N'Nancy', N'Sales Manager', 1, '1958/12/8', '2002/5/1', N'825 8 Ave SW', N'Calgary', N'AB', N'Canada', N'T2P 2T3', N'+1 (403) 262-3443', N'+1 (403) 262-3322', N'nancy@chinookcorp.com'),
    (N'Peacock', N'Jane', N'Sales Support Agent', 2, '1973/8/29', '2002/4/1', N'1111 6 Ave SW', N'Calgary', N'AB', N'Canada', N'T2P 5M5', N'+1 (403) 262-3443', N'+1 (403) 262-6712', N'jane@chinookcorp.com'),
    (N'Park', N'Margaret', N'Sales Support Agent', 2, '1947/9/19', '2003/5/3', N'683 10 Street SW', N'Calgary', N'AB', N'Canada', N'T2P 5G3', N'+1 (403) 263-4423', N'+1 (403) 263-4289', N'margaret@chinookcorp.com'),
    (N'Johnson', N'Steve', N'Sales Support Agent', 2, '1965/3/3', '2003/10/17', N'7727B 41 Ave', N'Calgary', N'AB', N'Canada', N'T3B 1Y7', N'1 (780) 836-9987', N'1 (780) 836-9543', N'steve@chinookcorp.com'),
    (N'Mitchell', N'Michael', N'IT Manager', 1, '1973/7/1', '2003/10/17', N'5827 Bowness Road NW', N'Calgary', N'AB', N'Canada', N'T3B 0C5', N'+1 (403) 246-9887', N'+1 (403) 246-9899', N'michael@chinookcorp.com'),
    (N'King', N'Robert', N'IT Staff', 6, '1970/5/29', '2004/1/2', N'590 Columbia Boulevard West', N'Lethbridge', N'AB', N'Canada', N'T1K 5N8', N'+1 (403) 456-9986', N'+1 (403) 456-8485', N'robert@chinookcorp.com'),
    (N'Callahan', N'Laura', N'IT Staff', 6, '1968/1/9', '2004/3/4', N'923 7 ST NW', N'Lethbridge', N'AB', N'Canada', N'T1H 1Y8', N'+1 (403) 467-3351', N'+1 (403) 467-8772', N'laura@chinookcorp.com');

INSERT INTO customer (first_name, last_name, company, address, city, state, country, postal_code, phone, fax, email, support_rep_id) VALUES
    (N'Luís', N'Gonçalves', N'Embraer - Empresa Brasileira de Aeronáutica S.A.', N'Av. Brigadeiro Faria Lima, 2170', N'São José dos Campos', N'SP', N'Brazil', N'12227-000', N'+55 (12) 3923-5555', N'+55 (12) 3923-5566', N'luisg@embraer.com.br', 3),
    (N'Leonie', N'Köhler', NULL, N'Theodor-Heuss-Straße 34', N'Stuttgart', NULL, N'Germany', N'70174', N'+49 0711 2842222', NULL, N'leonekohler@surfeu.de', 5),
    (N'François', N'Tremblay', NULL, N'1498 rue Bélanger', N'Montréal', N'QC', N'Canada', N'H2G 1A7', N'+1 (514) 721-4711', NULL, N'ftremblay@gmail.com', 3),
    (N'Bjørn', N'Hansen', NULL, N'Ullevålsveien 14', N'Oslo', NULL, N'Norway', N'0171', N'+47 22 44 22 22', NULL, N'bjorn.hansen@yahoo.no', 4),
    (N'František', N'Wichterlová', N'JetBrains s.r.o.', N'Klanova 9/506', N'Prague', NULL, N'Czech Republic', N'14700', N'+420 2 4172 5555', N'+420 2 4172 5555', N'frantisekw@jetbrains.com', 4),
    (N'Helena', N'Holý', NULL, N'Rilská 3174/6', N'Prague', NULL, N'Czech Republic', N'14300', N'+420 2 4177 0449', NULL, N'hholy@gmail.com', 5),
    (N'Astrid', N'Gruber', NULL, N'Rotenturmstraße 4, 1010 Innere Stadt', N'Vienne', NULL, N'Austria', N'1010', N'+43 01 5134505', NULL, N'astrid.gruber@apple.at', 5),
    (N'Daan', N'Peeters', NULL, N'Grétrystraat 63', N'Brussels', NULL, N'Belgium', N'1000', N'+32 02 219 03 03', NULL, N'daan_peeters@apple.be', 4),
    (N'Kara', N'Nielsen', NULL, N'Sønder Boulevard 51', N'Copenhagen', NULL, N'Denmark', N'1720', N'+453 3331 9991', NULL, N'kara.nielsen@jubii.dk', 4),
    (N'Eduardo', N'Martins', N'Woodstock Discos', N'Rua Dr. Falcão Filho, 155', N'São Paulo', N'SP', N'Brazil', N'01007-010', N'+55 (11) 3033-5446', N'+55 (11) 3033-4564', N'eduardo@woodstock.com.br', 4),
    (N'Alexandre', N'Rocha', N'Banco do Brasil S.A.', N'Av. Paulista, 2022', N'São Paulo', N'SP', N'Brazil', N'01310-200', N'+55 (11) 3055-3278', N'+55 (11) 3055-8131', N'alero@uol.com.br', 5),
    (N'Roberto', N'Almeida', N'Riotur', N'Praça Pio X, 119', N'Rio de Janeiro', N'RJ', N'Brazil', N'20040-020', N'+55 (21) 2271-7000', N'+55 (21) 2271-7070', N'roberto.almeida@riotur.gov.br', 3),
    (N'Fernanda', N'Ramos', NULL, N'Qe 7 Bloco G', N'Brasília', N'DF', N'Brazil', N'71020-677', N'+55 (61) 3363-5547', N'+55 (61) 3363-7855', N'fernadaramos4@uol.com.br', 4),
    (N'Mark', N'Philips', N'Telus', N'8210 111 ST NW', N'Edmonton', N'AB', N'Canada', N'T6G 2C7', N'+1 (780) 434-4554', N'+1 (780) 434-5565', N'mphilips12@shaw.ca', 5),
    (N'Jennifer', N'Peterson', N'Rogers Canada', N'700 W Pender Street', N'Vancouver', N'BC', N'Canada', N'V6C 1G8', N'+1 (604) 688-2255', N'+1 (604) 688-8756', N'jenniferp@rogers.ca', 3),
    (N'Frank', N'Harris', N'Google Inc.', N'1600 Amphitheatre Parkway', N'Mountain View', N'CA', N'USA', N'94043-1351', N'+1 (650) 253-0000', N'+1 (650) 253-0000', N'fharris@google.com', 4),
    (N'Jack', N'Smith', N'Microsoft Corporation', N'1 Microsoft Way', N'Redmond', N'WA', N'USA', N'98052-8300', N'+1 (425) 882-8080', N'+1 (425) 882-8081', N'jacksmith@microsoft.com', 5),
    (N'Michelle', N'Brooks', NULL, N'627 Broadway', N'New York', N'NY', N'USA', N'10012-2612', N'+1 (212) 221-3546', N'+1 (212) 221-4679', N'michelleb@aol.com', 3),
    (N'Tim', N'Goyer', N'Apple Inc.', N'1 Infinite Loop', N'Cupertino', N'CA', N'USA', N'95014', N'+1 (408) 996-1010', N'+1 (408) 996-1011', N'tgoyer@apple.com', 3),
    (N'Dan', N'Miller', NULL, N'541 Del Medio Avenue', N'Mountain View', N'CA', N'USA', N'94040-111', N'+1 (650) 644-3358', NULL, N'dmiller@comcast.com', 4),
    (N'Kathy', N'Chase', NULL, N'801 W 4th Street', N'Reno', N'NV', N'USA', N'89503', N'+1 (775) 223-7665', NULL, N'kachase@hotmail.com', 5),
    (N'Heather', N'Leacock', NULL, N'120 S Orange Ave', N'Orlando', N'FL', N'USA', N'32801', N'+1 (407) 999-7788', NULL, N'hleacock@gmail.com', 4),
    (N'John', N'Gordon', NULL, N'69 Salem Street', N'Boston', N'MA', N'USA', N'2113', N'+1 (617) 522-1333', NULL, N'johngordon22@yahoo.com', 4),
    (N'Frank', N'Ralston', NULL, N'162 E Superior Street', N'Chicago', N'IL', N'USA', N'60611', N'+1 (312) 332-3232', NULL, N'fralston@gmail.com', 3),
    (N'Victor', N'Stevens', NULL, N'319 N. Frances Street', N'Madison', N'WI', N'USA', N'53703', N'+1 (608) 257-0597', NULL, N'vstevens@yahoo.com', 5),
    (N'Richard', N'Cunningham', NULL, N'2211 W Berry Street', N'Fort Worth', N'TX', N'USA', N'76110', N'+1 (817) 924-7272', NULL, N'ricunningham@hotmail.com', 4),
    (N'Patrick', N'Gray', NULL, N'1033 N Park Ave', N'Tucson', N'AZ', N'USA', N'85719', N'+1 (520) 622-4200', NULL, N'patrick.gray@aol.com', 4),
    (N'Julia', N'Barnett', NULL, N'302 S 700 E', N'Salt Lake City', N'UT', N'USA', N'84102', N'+1 (801) 531-7272', NULL, N'jubarnett@gmail.com', 5),
    (N'Robert', N'Brown', NULL, N'796 Dundas Street West', N'Toronto', N'ON', N'Canada', N'M6J 1V1', N'+1 (416) 363-8888', NULL, N'robbrown@shaw.ca', 3),
    (N'Edward', N'Francis', NULL, N'230 Elgin Street', N'Ottawa', N'ON', N'Canada', N'K2P 1L7', N'+1 (613) 234-3322', NULL, N'edfrancis@yachoo.ca', 3),
    (N'Martha', N'Silk', NULL, N'194A Chain Lake Drive', N'Halifax', N'NS', N'Canada', N'B3S 1C5', N'+1 (902) 450-0450', NULL, N'marthasilk@gmail.com', 5),
    (N'Aaron', N'Mitchell', NULL, N'696 Osborne Street', N'Winnipeg', N'MB', N'Canada', N'R3L 2B9', N'+1 (204) 452-6452', NULL, N'aaronmitchell@yahoo.ca', 4),
    (N'Ellie', N'Sullivan', NULL, N'5112 48 Street', N'Yellowknife', N'NT', N'Canada', N'X1A 1N6', N'+1 (867) 920-2233', NULL, N'ellie.sullivan@shaw.ca', 3),・・・

INSERT INTO invoice (customer_id, invoice_date, billing_address, billing_city, billing_state, billing_country, billing_postal_code, total) VALUES
    (2, '2021/1/1', N'Theodor-Heuss-Straße 34', N'Stuttgart', NULL, N'Germany', N'70174', 1.98),
    (4, '2021/1/2', N'Ullevålsveien 14', N'Oslo', NULL, N'Norway', N'0171', 3.96),
    (8, '2021/1/3', N'Grétrystraat 63', N'Brussels', NULL, N'Belgium', N'1000', 5.94),
    (14, '2021/1/6', N'8210 111 ST NW', N'Edmonton', N'AB', N'Canada', N'T6G 2C7', 8.91),
    (23, '2021/1/11', N'69 Salem Street', N'Boston', N'MA', N'USA', N'2113', 13.86),
    (37, '2021/1/19', N'Berger Straße 10', N'Frankfurt', NULL, N'Germany', N'60316', 0.99),
    (38, '2021/2/1', N'Barbarossastraße 19', N'Berlin', NULL, N'Germany', N'10779', 1.98),
    (40, '2021/2/1', N'8, Rue Hanovre', N'Paris', NULL, N'France', N'75002', 1.98),
    (42, '2021/2/2', N'9, Place Louis Barthou', N'Bordeaux', NULL, N'France', N'33000', 3.96),
    (46, '2021/2/3', N'3 Chatham Street', N'Dublin', N'Dublin', N'Ireland', NULL, 5.94),
    (52, '2021/2/6', N'202 Hoxton Street', N'London', NULL, N'United Kingdom', N'N1 5LH', 8.91),
    (2, '2021/2/11', N'Theodor-Heuss-Straße 34', N'Stuttgart', NULL, N'Germany', N'70174', 13.86),
    (16, '2021/2/19', N'1600 Amphitheatre Parkway', N'Mountain View', N'CA', N'USA', N'94043-1351', 0.99),
    (17, '2021/3/4', N'1 Microsoft Way', N'Redmond', N'WA', N'USA', N'98052-8300', 1.98),
    (19, '2021/3/4', N'1 Infinite Loop', N'Cupertino', N'CA', N'USA', N'95014', 1.98),
    (21, '2021/3/5', N'801 W 4th Street', N'Reno', N'NV', N'USA', N'89503', 3.96),
    (25, '2021/3/6', N'319 N. Frances Street', N'Madison', N'WI', N'USA', N'53703', 5.94),
    (31, '2021/3/9', N'194A Chain Lake Drive', N'Halifax', N'NS', N'Canada', N'B3S 1C5', 8.91),
    (40, '2021/3/14', N'8, Rue Hanovre', N'Paris', NULL, N'France', N'75002', 13.86),
    (54, '2021/3/22', N'110 Raeburn Pl', N'Edinburgh ', NULL, N'United Kingdom', N'EH4 1HH', 0.99),
    (55, '2021/4/4', N'421 Bourke Street', N'Sidney', N'NSW', N'Australia', N'2010', 1.98),
    (57, '2021/4/4', N'Calle Lira, 198', N'Santiago', NULL, N'Chile', NULL, 1.98),
    (59, '2021/4/5', N'3,Raj Bhavan Road', N'Bangalore', NULL, N'India', N'560001', 3.96),
    (4, '2021/4/6', N'Ullevålsveien 14', N'Oslo', NULL, N'Norway', N'0171', 5.94),
    (10, '2021/4/9', N'Rua Dr. Falcão Filho, 155', N'São Paulo', N'SP', N'Brazil', N'01007-010', 8.91),
    (19, '2021/4/14', N'1 Infinite Loop', N'Cupertino', N'CA', N'USA', N'95014', 13.86),
    (33, '2021/4/22', N'5112 48 Street', N'Yellowknife', N'NT', N'Canada', N'X1A 1N6', 0.99),
    (34, '2021/5/5', N'Rua da Assunção 53', N'Lisbon', NULL, N'Portugal', NULL, 1.98),
    (36, '2021/5/5', N'Tauentzienstraße 8', N'Berlin', NULL, N'Germany', N'10789', 1.98),
    (38, '2021/5/6', N'Barbarossastraße 19', N'Berlin', NULL, N'Germany', N'10779', 3.96),
    (42, '2021/5/7', N'9, Place Louis Barthou', N'Bordeaux', NULL, N'France', N'33000', 5.94),
    (48, '2021/5/10', N'Lijnbaansgracht 120bg', N'Amsterdam', N'VV', N'Netherlands', N'1016', 8.91),
    (57, '2021/5/15', N'Calle Lira, 198', N'Santiago', NULL, N'Chile', NULL, 13.86),
    (12, '2021/5/23', N'Praça Pio X, 119', N'Rio de Janeiro', N'RJ', N'Brazil', N'20040-020', 0.99),
    (13, '2021/6/5', N'Qe 7 Bloco G', N'Brasília', N'DF', N'Brazil', N'71020-677', 1.98),
    (15, '2021/6/5', N'700 W Pender Street', N'Vancouver', N'BC', N'Canada', N'V6C 1G8', 1.98),
    (17, '2021/6/6', N'1 Microsoft Way', N'Redmond', N'WA', N'USA', N'98052-8300', 3.96),
    (21, '2021/6/7', N'801 W 4th Street', N'Reno', N'NV', N'USA', N'89503', 5.94),
    (27, '2021/6/10', N'1033 N Park Ave', N'Tucson', N'AZ', N'USA', N'85719', 8.91),
    (36, '2021/6/15', N'Tauentzienstraße 8', N'Berlin', NULL, N'Germany', N'10789', 13.86),
    (50, '2021/6/23', N'C/ San Bernardo 85', N'Madrid', NULL, N'Spain', N'28015', 0.99),
    (51, '2021/7/6', N'Celsiusg. 9', N'Stockholm', NULL, N'Sweden', N'11230', 1.98),
    (53, '2021/7/6', N'113 Lupus St', N'London', NULL, N'United Kingdom', N'SW1V 3EN', 1.98),
    (55, '2021/7/7', N'421 Bourke Street', N'Sidney', N'NSW', N'Australia', N'2010', 3.96),
    (59, '2021/7/8', N'3,Raj Bhavan Road', N'Bangalore', NULL, N'India', N'560001', 5.94),
    (6, '2021/7/11', N'Rilská 3174/6', N'Prague', NULL, N'Czech Republic', N'14300', 8.91),
    (15, '2021/7/16', N'700 W Pender Street', N'Vancouver', N'BC', N'Canada', N'V6C 1G8', 13.86),・・・
INSERT INTO invoice_line (invoice_id, track_id, unit_price, quantity) VALUES
    (1, 2, 0.99, 1),
    (1, 4, 0.99, 1),
    (2, 6, 0.99, 1),
    (2, 8, 0.99, 1),
    (2, 10, 0.99, 1),
    (2, 12, 0.99, 1),
    (3, 16, 0.99, 1),
    (3, 20, 0.99, 1),
    (3, 24, 0.99, 1),
    (3, 28, 0.99, 1),
    (3, 32, 0.99, 1),
    (3, 36, 0.99, 1),
    (4, 42, 0.99, 1),
    (4, 48, 0.99, 1),
    (4, 54, 0.99, 1),
    (4, 60, 0.99, 1),
    (4, 66, 0.99, 1),
    (4, 72, 0.99, 1),・・・

INSERT INTO playlist (name) VALUES
    (N'Music'),
    (N'Movies'),
    (N'TV Shows'),
    (N'Audiobooks'),
    (N'90’s Music'),
    (N'Audiobooks'),
    (N'Movies'),
    (N'Music'),
    (N'Music Videos'),
    (N'TV Shows'),
    (N'Brazilian Music'),
    (N'Classical'),
    (N'Classical 101 - Deep Cuts'),
    (N'Classical 101 - Next Steps'),
    (N'Classical 101 - The Basics'),
    (N'Grunge'),
    (N'Heavy Metal Classic'),
    (N'On-The-Go 1');

INSERT INTO playlist_track (playlist_id, track_id) VALUES
    (1, 3402),
    (1, 3389),
    (1, 3390),
    (1, 3391),
    (1, 3392),
    (1, 3393),
    (1, 3394),
    (1, 3395),
    (1, 3396),
    (1, 3397),
    (1, 3398),
    (1, 3399),
    (1, 3400),
    (1, 3401),
    (1, 3336),
    (1, 3478),
    (1, 3375),
    (1, 3376),
    (1, 3377),
    (1, 3378),
    (1, 3379),
    (1, 3380),
    (1, 3381),・・・

```