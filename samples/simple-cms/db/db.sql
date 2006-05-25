-- MySQL Administrator dump 1.4
--
-- ------------------------------------------------------
-- Server version	4.1.14-nt


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


--
-- Create schema gcms
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ gcms;
USE gcms;

--
-- Table structure for table `gcms`.`accesskey`
--

DROP TABLE IF EXISTS `accesskey`;
CREATE TABLE `accesskey` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `CODE` varchar(255) NOT NULL default '',
  `EXPIRYDATE` datetime NOT NULL default '0000-00-00 00:00:00',
  `STARTDATE` datetime NOT NULL default '0000-00-00 00:00:00',
  `ROLE_ID` bigint(20) default NULL,
  `ENDDATE` datetime NOT NULL default '0000-00-00 00:00:00',
  `USAGES` int(11) NOT NULL default '0',
  `CREATED` datetime default NULL,
  `SITE_ID` bigint(20) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK28074E5B52388A1A` (`ROLE_ID`),
  KEY `FK28074E5B7D52CC7A` (`SITE_ID`),
  CONSTRAINT `FK28074E5B52388A1A` FOREIGN KEY (`ROLE_ID`) REFERENCES `role` (`ID`),
  CONSTRAINT `FK28074E5B7D52CC7A` FOREIGN KEY (`SITE_ID`) REFERENCES `site` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`answer`
--

DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `TEXT` varchar(255) NOT NULL default '',
  `CORRECT` tinyint(1) NOT NULL default '0',
  `QUESTION_ID` bigint(20) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK735D33BEF7D6841A` (`QUESTION_ID`),
  CONSTRAINT `FK735D33BEF7D6841A` FOREIGN KEY (`QUESTION_ID`) REFERENCES `question` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`comment`
--

DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `MESSAGE` varchar(255) NOT NULL default '',
  `LEFTBY_ID` bigint(20) default NULL,
  `REVISION_ID` bigint(20) default NULL,
  `LEFTON` datetime default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK63717A3F68291E47` (`LEFTBY_ID`),
  KEY `FK63717A3F12F003FA` (`REVISION_ID`),
  CONSTRAINT `FK63717A3F12F003FA` FOREIGN KEY (`REVISION_ID`) REFERENCES `revision` (`ID`),
  CONSTRAINT `FK63717A3F68291E47` FOREIGN KEY (`LEFTBY_ID`) REFERENCES `user` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`message`
--

DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `DATEPOSTED` datetime NOT NULL default '0000-00-00 00:00:00',
  `TITLE` varchar(255) NOT NULL default '',
  `TOPIC_ID` bigint(20) default NULL,
  `MESSAGE` text NOT NULL,
  `FORUM_ID` bigint(20) default NULL,
  `BY_ID` bigint(20) NOT NULL default '0',
  PRIMARY KEY  (`ID`),
  KEY `FK63B68BE75C5D1EE` (`BY_ID`),
  KEY `FK63B68BE7CBC4BF3A` (`TOPIC_ID`),
  KEY `FK63B68BE71CDD9BA8` (`FORUM_ID`),
  CONSTRAINT `FK63B68BE71CDD9BA8` FOREIGN KEY (`FORUM_ID`) REFERENCES `page` (`ID`),
  CONSTRAINT `FK63B68BE75C5D1EE` FOREIGN KEY (`BY_ID`) REFERENCES `user` (`ID`),
  CONSTRAINT `FK63B68BE7CBC4BF3A` FOREIGN KEY (`TOPIC_ID`) REFERENCES `topic` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`page`
--

DROP TABLE IF EXISTS `page`;
CREATE TABLE `page` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `TYPE` varchar(255) NOT NULL default '',
  `TITLE` varchar(255) NOT NULL default '',
  `SITE_ID` bigint(20) default NULL,
  `POSITION` int(11) NOT NULL default '0',
  `TEMPLATE` varchar(255) NOT NULL default '',
  `CREATEDBY_ID` bigint(20) default NULL,
  `CONTENT` text NOT NULL,
  `PAGE_ID` bigint(20) default NULL,
  `ENABLED` tinyint(1) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK255A8F77BA6406` (`CREATEDBY_ID`),
  KEY `FK255A8FD040A77A` (`PAGE_ID`),
  KEY `FK255A8F7D52CC7A` (`SITE_ID`),
  CONSTRAINT `FK255A8F77BA6406` FOREIGN KEY (`CREATEDBY_ID`) REFERENCES `user` (`ID`),
  CONSTRAINT `FK255A8F7D52CC7A` FOREIGN KEY (`SITE_ID`) REFERENCES `site` (`ID`),
  CONSTRAINT `FK255A8FD040A77A` FOREIGN KEY (`PAGE_ID`) REFERENCES `page` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`question`
--

DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `QUESTIONNAIRE_ID` bigint(20) default NULL,
  `ANSWERS` varchar(255) NOT NULL default '',
  `TEXT` text NOT NULL,
  `TITLE` varchar(255) NOT NULL default '',
  `SCORE` int(11) NOT NULL default '0',
  `NUMBER` int(11) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FKE9282BE67B5933E6` (`QUESTIONNAIRE_ID`),
  CONSTRAINT `FKE9282BE67B5933E6` FOREIGN KEY (`QUESTIONNAIRE_ID`) REFERENCES `page` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`revision`
--

DROP TABLE IF EXISTS `revision`;
CREATE TABLE `revision` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `UPDATEDBY_ID` bigint(20) default NULL,
  `PAGE_ID` bigint(20) default NULL,
  `LASTUPDATED` datetime NOT NULL default '0000-00-00 00:00:00',
  `CONTENT` varchar(255) NOT NULL default '',
  `STATE` varchar(255) NOT NULL default '',
  `NUMBER` float NOT NULL default '0',
  PRIMARY KEY  (`ID`),
  KEY `FK1F1AA7DBE4252E13` (`UPDATEDBY_ID`),
  KEY `FK1F1AA7DBD040A77A` (`PAGE_ID`),
  CONSTRAINT `FK1F1AA7DBD040A77A` FOREIGN KEY (`PAGE_ID`) REFERENCES `page` (`ID`),
  CONSTRAINT `FK1F1AA7DBE4252E13` FOREIGN KEY (`UPDATEDBY_ID`) REFERENCES `user` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`role`
--

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `SITE_ID` bigint(20) default NULL,
  `NAME` varchar(255) NOT NULL default '',
  `TITLE` varchar(255) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK2678767D52CC7A` (`SITE_ID`),
  CONSTRAINT `FK2678767D52CC7A` FOREIGN KEY (`SITE_ID`) REFERENCES `site` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`site`
--

DROP TABLE IF EXISTS `site`;
CREATE TABLE `site` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `HOMEPAGE_ID` bigint(20) default NULL,
  `DOMAIN` varchar(255) NOT NULL default '',
  `NAME` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`ID`),
  KEY `FK26D74755FB49BB` (`HOMEPAGE_ID`),
  CONSTRAINT `FK26D74755FB49BB` FOREIGN KEY (`HOMEPAGE_ID`) REFERENCES `page` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`test`
--

DROP TABLE IF EXISTS `test`;
CREATE TABLE `test` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`topic`
--

DROP TABLE IF EXISTS `topic`;
CREATE TABLE `topic` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `TITLE` varchar(255) NOT NULL default '',
  `FORM_ID` bigint(20) default NULL,
  `DESCRIPTION` varchar(255) NOT NULL default '',
  `DATEADDED` datetime default NULL,
  `FORUM_ID` bigint(20) default NULL,
  `CREATEDBY_ID` bigint(20) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK4C4D50FD7C4D525` (`FORM_ID`),
  KEY `FK4C4D50F1CDD9BA8` (`FORUM_ID`),
  KEY `FK4C4D50F77BA6406` (`CREATEDBY_ID`),
  CONSTRAINT `FK4C4D50F1CDD9BA8` FOREIGN KEY (`FORUM_ID`) REFERENCES `page` (`ID`),
  CONSTRAINT `FK4C4D50F77BA6406` FOREIGN KEY (`CREATEDBY_ID`) REFERENCES `user` (`ID`),
  CONSTRAINT `FK4C4D50FD7C4D525` FOREIGN KEY (`FORM_ID`) REFERENCES `page` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`user`
--

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `TITLE` varchar(255) NOT NULL default '',
  `LOGIN` varchar(255) NOT NULL default '',
  `ROLE_ID` bigint(20) default NULL,
  `EMAIL` varchar(255) NOT NULL default '',
  `FIRSTNAME` varchar(255) NOT NULL default '',
  `PWD` varchar(255) NOT NULL default '',
  `LASTNAME` varchar(255) NOT NULL default '',
  `COMPANY` varchar(255) NOT NULL default '',
  `JOBTITLE` varchar(255) default NULL,
  `ACCESSKEY_ID` bigint(20) default NULL,
  `INDUSTRYSECTOR` varchar(255) default NULL,
  `JOBFUNCTION` varchar(255) default NULL,
  `COUNTRY` varchar(255) default NULL,
  `ACTIVE` tinyint(1) default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK27E3CB52388A1A` (`ROLE_ID`),
  KEY `FK27E3CB997A439A` (`ACCESSKEY_ID`),
  CONSTRAINT `FK27E3CB52388A1A` FOREIGN KEY (`ROLE_ID`) REFERENCES `role` (`ID`),
  CONSTRAINT `FK27E3CB997A439A` FOREIGN KEY (`ACCESSKEY_ID`) REFERENCES `accesskey` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `gcms`.`userresults`
--

DROP TABLE IF EXISTS `userresults`;
CREATE TABLE `userresults` (
  `ID` bigint(20) NOT NULL auto_increment,
  `VERSION` bigint(20) NOT NULL default '0',
  `QUESTIONNAIRE_ID` bigint(20) default NULL,
  `USER_ID` bigint(20) default NULL,
  `ANSWER_ID` bigint(20) default NULL,
  `QUESTION_ID` bigint(20) default NULL,
  `CREATED` datetime default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK4C8819ABF7634DFA` (`USER_ID`),
  KEY `FK4C8819AB7B5933E6` (`QUESTIONNAIRE_ID`),
  KEY `FK4C8819ABDA205D1A` (`ANSWER_ID`),
  KEY `FK4C8819ABF7D6841A` (`QUESTION_ID`),
  CONSTRAINT `FK4C8819AB7B5933E6` FOREIGN KEY (`QUESTIONNAIRE_ID`) REFERENCES `page` (`ID`),
  CONSTRAINT `FK4C8819ABDA205D1A` FOREIGN KEY (`ANSWER_ID`) REFERENCES `answer` (`ID`),
  CONSTRAINT `FK4C8819ABF7634DFA` FOREIGN KEY (`USER_ID`) REFERENCES `user` (`ID`),
  CONSTRAINT `FK4C8819ABF7D6841A` FOREIGN KEY (`QUESTION_ID`) REFERENCES `question` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
