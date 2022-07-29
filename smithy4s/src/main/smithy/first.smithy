namespace foo

list IntList {
    member: Integer
}

//set StringSet {
//    member: Set
//}

structure Person {
    @required
    firstName: String,
    middleName: String,
    @required
    lastName: String,
    dateOfBirth: Timestamp
}
