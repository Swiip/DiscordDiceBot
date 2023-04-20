alter table CHANNEL_CONFIG
    drop primary key;

ALTER TABLE CHANNEL_CONFIG
    ADD PRIMARY KEY (CONFIG_ID);

update CHANNEL_CONFIG
set USER_ID = -1
where USER_ID is null;

ALTER TABLE CHANNEL_CONFIG ALTER COLUMN USER_ID SET NOT NULL;

create unique index CHANNEL_CONFIG_CI_CCI_UI on CHANNEL_CONFIG (CHANNEL_ID, CONFIG_CLASS_ID, USER_ID);
create index CHANNEL_CONFIG_CI_CCI on CHANNEL_CONFIG (CHANNEL_ID, CONFIG_CLASS_ID);
