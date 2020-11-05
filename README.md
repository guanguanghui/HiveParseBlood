# HiveParseBlood
sql语法解析在数据血缘追踪上的运用


# 1.背景

数据血缘关系简单的说就是数据之间的上下游来源去向关系，涉及到数据输入源输出源。数据血缘关系重要作用不言而喻，比如：如果一个数据有问题，可以根据血缘关系往上游排查，看看到底在哪个环节出了问题。此外也可以通过数据的血缘关系，建立起生产这些数据的任务之间的依赖关系，进而辅助调度系统的工作调度，或者用来判断一个失败或错误的任务可能对哪些下游数据造成影响等等。随着数据仓库接入的表和建立的模型增多，元数据管理就变得越来越重要，元数据表血缘关系维护表与表之间的关系。良好的元数据管理，可以清晰和明确看出每张表和模型之前的关系。元数据的血缘关系挖掘，对数据流向追踪、业务问题排查、减少维护成本、提升开发效率等起着十分重要的作用。

目前，数仓经常会碰到以下几类问题：

1、两个数据报表进行对比，结果差异很大，需要人工核对分析指标的维度信息，比如从头分析数据指标从哪里来，处理条件是什么，最后才能分析出问题原因。

2、基础数据表因某种原因需要修改字段，需要评估其对数仓的影响，费时费力，然后在做方案。目前，只能依靠手工维护，一旦脚本发生变化，手工维护遗漏或不及时，就会造成关系不准确。因此，如何有效的完成各个数据表、字段之间的关系梳理，分析出数据的血缘关系，是一项亟待解决的问题。

# 2.语法解析

语法解析常用的实现分为两种:

1.parser generator，就是你写好规则，自动帮你生成解析代码，比如常见的antlr就是。这种方式的特写是性能高，效率高。缺点则是定制化扩展弱，异常信息非常难读懂。

2.parser combinator，这种就是你自己手写语法规则，非常灵活，效率会低一点，可控性比较强。



语法解析的目地是生成AST，简单来说就是一个递归的数据结构，有点类似json。在语法解析过程中要做的工作就是，定义数据结构，然后把信息塞进去。其实难度并不大，但是由于sql规则的复杂度，整个工作量并不轻松。



语法规则解析过程分为两部分：词法解析及语法解析。

为什么需要词法解析呢？因为sql跟平常我们所说的csv文件并不一样，支持可以使用分隔符来定义结构。有些东西在不同的环境下有不同的意义。比如任何事物在注释里面它就没有效果了，里面可能还有变量声明，关键字信息等。所以词法分析器就是按sql的基本单元进行拆分，然后语法分析器将其组装起来形成递归的结构体。

![图片](https://uploader.shimo.im/f/ZUqYSA1JlycIAyW2.png!thumbnail)

可以看出，hive的基本单元分为了TokWord(通用名称), TokString(字符), TokNumber(数字), TokSymbol(符号), TokVariable(变量)，这个解析的过程叫做scanner，就是常用的字符处理生成Token流。

有了Token流，我们进行组合生成AST。

![图片](https://uploader.shimo.im/f/tnDPN3dYkDG3MCj6.png!thumbnail)

接下来就是把token组装起来。解析过程并不复杂，就是按结构体深度递归，跟json解析差不了多少。整个过程就是，至上而下解析，解析一项，如果失败了则尝试其它项，深度递归下去。

体力活比较重，因为规则多，每一项都要写规则尝试匹配。就这样，整个结构体就组装起来了。

![图片](https://uploader.shimo.im/f/Ubhjb60d1gayIQfC.png!thumbnail)

# 3.从语法解析到数据血缘关系

基于元数据的数据血缘关系分析方法，能够有效的完成各个数据表、字段之间的关系梳理，分析出数据的血缘关系。

**从语法解析到数据血缘关系大致包括以下步骤：**

**1.**通过开源语法分析器定义结构化查询语言的词法规则和语法规则，并对结构化查询语言的词法规则和语法规则进行解析，将结构化查询语言转化为抽象语法树；

具体地，首先通过Antlr(Antlr是可以根据输入自动生成语法树并可视化的显示出来的开源语法分析器)对HQL的词法规则和语法规则进行解析，将SQL转化为抽象语法树ASTTree。

**2.**遍历抽象语法树，抽象出查询的基本组成单元；

然后，遍历AST Tree，抽象出查询的基本组成单元QueryBlock。

遍历查询的基本组成单元，生成执行操作树；

在抽象出查询的基本组成单元QueryBlock后，生成逻辑执行计划，即，遍历QueryBlock，翻译为执行操作树OperatorTree。

**3.**QueryBlock是一条SQL最基本的组成单元，包括三个部分：输入源，计算过程，输出。分析出输入输出表、字段和相应的处理条件，作为元数据管理和Hive表的血缘分析信息。

**更详细具体的步骤如下：**

具体的，对AST深度优先遍历，遇到操作的token则判断当前的操作，遇到子句则压栈当前处理，处理子句。子句处理完，栈弹出。处理字句的过程中，遇到子查询就保存当前子查询的信息，判断与其父查询的关系，最终形成树形结构；遇到字段或者条件处理则记录当前的字段和条件信息、组成Block，嵌套调用。

Hive使用Antlr实现SQL的词法和语法解析。只需要了解使用Antlr构造特定的语言只需要编写一个语法文件，定义词法和语法替换规则即可，Antlr完成了词法分析、语法分析、语义分析、中间代码生成的过程。解析SQL的方案，以Hive为例。先定义词法规则和语法规则文件，然后使用Antlr实现SQL的词法和语法解析，生成AST语法树，遍历AST语法树完成后续操作。经过词法和语法解析后，如果需要对表达式做进一步的处理，使用Antlr的抽象语法树语法Abstract Syntax Tree，在语法分析的同时将输入语句转换成抽象语法树，后续在遍历语法树时完成进一步的处理。Antlr对Hive SQL解析的代码如下，HiveLexerX，HiveParser分别是Antlr对语法文件Hive.g编译后自动生成的词法解析和语法解析类，在这两个类中进行复杂的解析。

需要说明的是，内层子查询也会生成一个TOK_DESTINATION节点，这个节点是在语法改写中特意增加了的一个节点。原因是Hive中所有查询的数据均会保存在HDFS临时的文件中，无论是中间的子查询还是查询最终的结果，Insert语句最终会将数据写入表所在的HDFS目录下。详细来看，将内存子查询的from子句展开后，得到如下AST Tree，每个表生成一个TOK_TABREF节点，Join条件生成一个“＝”节点。其他SQL部分类似，不一一详述。

AST Tree转化为QueryBlock就是将SQL进一部抽象和结构化。QueryBlock是一条SQL最基本的组成单元，包括三个部分：输入源，计算过程，输出。简单来讲一个QueryBlock就是一个子查询。AST Tree生成QueryBlock的过程是一个递归的过程，先序遍历AST Tree，遇到不同的Token节点，保存到相应的属性中，QueryBlock生成Operator Tree就是遍历上一个过程中生成的QB和QBParseInfo对象的保存语法的属性。大部分逻辑层优化器通过变换OperatorTree，合并操作符，达到减少MapReduce Job，减少shuffle数据量的目的。

# 4.数据血缘关系的表达-图

JGraphT支持数学图论对象和算法的免费Java图形库。JGraphT支持的图形包括：

* **有向图和无向图**
* 加权图、非加权图、带标签的图以及任何用户自定义边
* 支持各种多样性边属性：简单图、多重图和伪图（**pseudograph）**
* **对图只读访问：允许模块设置内部图形的“只读”访问**
* 图可监听：支持通过外部监听器跟踪修改事件
* **子图可根据其他图中的子视图变化自动更新**
* 支持上述所有功能组合使用

尽管功能强大，JGraphT的设计仍然非常简单并且（通过Java泛型）保证了类型安全。例如，图的顶点可以是任何对象。可以为String、URL和XML文档等等建图。你甚至可以为图元素本身创建图。

```xml
<dependency>
   <groupId>org.jgrapht</groupId>
   <artifactId>jgrapht-ext</artifactId>
   <version>1.2.0</version>
</dependency>
```
# 5.应用demo

**HQL：**

```sql
--1.表定义
create table temp.b1(id string, name string) row format delimited fields terminated by ',';
create table temp.b2(id string, age int) row format delimited fields terminated by ',';
create table temp.c1(id string, name string) row format delimited fields terminated by ',';
create table temp.c2(id string, age int) row format delimited fields terminated by ',';" ,
create table temp.d1(id string, name string, age int) row format delimited fields terminated by ',';
-- 2.表数据填充
insert into table temp.b1 select id, name from temp.a1；
insert into table temp.b2 select id, age from temp.a1;
insert overwrite table temp.c1 select id, name from temp.b1;
insert overwrite table temp.c2 select id, age from temp.b2;
insert overwrite table temp.d1 select t1.id, t1.name, t2.age from temp.c1 t1 join temp.c2 t2 on t1.id = t2.id;
insert overwrite table temp.e1 select id,name,age from (select t1.id, t1.name, t2.age from temp.b1 t1 join temp.b2 t2 on t1.id = t2.id union all select id,name,age from temp.d1) t;
create table temp.f1 as select id,name,age from (select t1.id, t1.name, t2.age from temp.b1 t1 join temp.b2 t2 on t1.id = t2.id union all select id,name,age from temp.d1) t;
```
**表血缘：**
![图片](https://uploader.shimo.im/f/INNo4wnloVJKJ205.png!thumbnail)![图片](https://uploader.shimo.im/f/dszlCVGPOB7D0nnT.png!thumbnail)

**表d1的字段血缘：**

![图片](https://uploader.shimo.im/f/S7AgpK9lLVWqywDa.png!thumbnail)



**demo代码：**


[HiveParseBlood.rar](https://uploader.shimo.im/f/SJh61U2zkaj9ZN5t.rar)



# 6.其他数据血缘关系的追踪工具

在HortonWorks Atlas和Cloudera Navigator中，主要通过计算框架自身支持的运行时hook来获得数据相关元数据和血缘相关信息，比如hive的hook是在语法解析阶段，storm的hook是在topology submit阶段。这么做的优点是血缘的追踪分析是基于真实运行任务的信息进行分析的，如果插件部署全面，也不太会有遗漏问题，但是这种方式也有很多不太好解决的问题，比如：

(1)如何更新一个历史上有依赖后来不再依赖的血缘关系；

(2)对于一个还未运行的任务，不能提前获取血缘信息；

(3)临时脚本或者错误的脚本逻辑对血缘关系数据的污染；

总之，基于运行时的信息来采集血缘关系，由于缺乏静态的业务信息辅助，如何甄别和更新血缘关系的生命周期和有效性会是一个棘手的问题，一定程度上也限制了应用的范围。

另一种方式的血缘信息的采集不是在运行时动进行的，而是配置定时任务形式，定期去采集所有在调度系统上配置的任务脚本。由于调度统一管理了所有用户的任务脚本，所以，可以对脚本进行静态的分析，加上脚本本身业务信息，执行情况和生命周期对开发平台是可知的，所以一定程度上能解决上述提到的几个问题。

# 7.参考

queryparser：一种sql语法解析工具

[https://github.com/uber/queryparser](https://github.com/uber/queryparser)

jgrapht：支持数学图论对象和算法的免费Java图形库

[https://github.com/jgrapht/jgrapht](https://github.com/jgrapht/jgrapht)

